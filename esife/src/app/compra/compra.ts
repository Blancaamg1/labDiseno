import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, PLATFORM_ID, ViewChild, inject } from '@angular/core';
import { Pagos } from '../pagos';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

declare global {
  interface Window {
    Stripe?: (publishableKey: string) => any;
  }
}

@Component({
  selector: 'app-compra',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './compra.html',
  styleUrl: './compra.css',
  host: {
    ngSkipHydration: 'true',
  },
})
export class CompraComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('cardElement') cardElementRef?: ElementRef<HTMLDivElement>;

  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  private readonly route = inject(ActivatedRoute, { optional: true });
  private readonly publishableKey = 'pk_test_51T92klDfoOsvKeXTdBJDJbXzaRbwz4oNNQF7pNsQbFjV0KLwxVwQvlCHIzXpcY4DEvYozxrSGxup0YGuaQyLYjWl00EHouwGZN';

  private stripe: any;
  private elements: any;
  private card: any;
  private isCardMounted = false;

  importe: number = 20.0;
  clientSecret?: string;
  cardError = '';
  isCardReady = false;
  isProcessing = false;
  showPaymentForm = false;
  idEspectaculo?: number;

  constructor(
    private service: Pagos,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    // Se evita acceder a snapshot en constructor para no romper en contextos sin ActivatedRoute.
    this.route?.queryParamMap.subscribe((params) => {
      this.setIdEspectaculo(params.get('idEspectaculo'));
    });

    // Fallback en navegador por si el componente se instancia fuera del flujo normal del Router.
    if (this.idEspectaculo === undefined && this.isBrowser) {
      this.setIdEspectaculo(new URLSearchParams(window.location.search).get('idEspectaculo'));
    }
  }

  ngAfterViewInit(): void {
    this.mountCardElement();
  }

  ngOnDestroy(): void {
    this.destroyCardElement();
  }

  irAPago() {
    let info = {
      centimos: Math.floor(this.importe * 100)
    };

    this.service.prepararPago(info).subscribe(
      (response) => {
        this.clientSecret = response?.toString().replace(/^"|"$/g, '');
        this.showPaymentForm = true;
      },
      (error) => {
        console.error('Error al preparar el pago:', error);
      }
    );
  }

  async onSubmitPayment(event: Event): Promise<void> {
    event.preventDefault();

    if (!this.card || !this.clientSecret || !this.isCardReady || this.isProcessing) {
      return;
    }

    this.isProcessing = true;
    this.cardError = '';

    try {
      const response = await this.stripe.confirmCardPayment(this.clientSecret, {
        payment_method: {
          card: this.card,
        },
      });

      if (response.error) {
        this.cardError = response.error.message || 'Error al procesar el pago.';
        return;
      }

      if (response.paymentIntent?.status === 'succeeded') {
        alert('Pago exitoso');
        const confirmPayload = {
          paymentIntentId: response.paymentIntent.id,
          clientSecret: this.clientSecret,
          userToken: localStorage.getItem('authToken'),
          // Este id llega al backend y evita guardar el Pago con idEspectaculo = 0.
          idEspectaculo: this.idEspectaculo,
        };

        this.service.confirmarPago(confirmPayload).subscribe({
          next: (serviceResponse: any) => {
            alert(serviceResponse?.mensaje || 'Pago confirmado');
          },
          error: (serviceError: any) => {
            const msg = serviceError?.error?.message || serviceError?.message || 'Error al confirmar el pago';
            alert(msg);
          },
        });
      }
    } finally {
      this.isProcessing = false;
    }
  }

  private ensureStripeInitialized(): boolean {
    if (!this.isBrowser) {
      return false;
    }

    if (!window.Stripe) {
      this.cardError = 'Stripe.js no esta disponible en el navegador.';
      return false;
    }

    if (!this.stripe) {
      this.stripe = window.Stripe(this.publishableKey);
      this.elements = this.stripe.elements();
    }

    return true;
  }

  private mountCardElement(): void {
    if (!this.ensureStripeInitialized()) {
      return;
    }

    if (this.isCardMounted) {
      return;
    }

    const host = this.cardElementRef?.nativeElement;
    if (!host) {
      return;
    }

    // Stripe exige montar el Element sobre un contenedor vacio.
    host.innerHTML = '';
    this.cardError = '';
    this.isCardReady = false;

    const style = {
      base: {
        color: '#32325d',
        fontFamily: 'Arial, sans-serif',
        fontSmoothing: 'antialiased',
        fontSize: '16px',
        '::placeholder': {
          color: '#32325d',
        },
      },
      invalid: {
        fontFamily: 'Arial, sans-serif',
        color: '#fa755a',
        iconColor: '#fa755a',
      },
    };

    this.card = this.elements.create('card', { style });

    this.card.on('ready', () => {
      this.isCardReady = true;
      this.cdr.detectChanges();
    });

    this.card.on('change', (event: any) => {
      this.cardError = event.error ? event.error.message : '';
    });

    this.card.mount(host);
    this.isCardMounted = true;
  }

  private destroyCardElement(): void {
    if (this.card) {
      this.card.unmount();
      this.card.destroy();
      this.card = undefined;
      this.isCardMounted = false;
    }
  }

  private setIdEspectaculo(rawId: string | null): void {
    if (rawId === null) {
      return;
    }

    const parsed = Number(rawId);
    if (!Number.isNaN(parsed) && parsed > 0) {
      this.idEspectaculo = parsed;
    }
  }
}