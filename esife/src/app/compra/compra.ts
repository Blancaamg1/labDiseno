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

  precioUnitario: number = 20.0;
  importe: number = 0;
  cantidadEntradas: number = 0;
  clientSecret?: string;
  cardError = '';
  isCardReady = false;
  isProcessing = false;
  showPaymentForm = false;
  idEspectaculo?: number;

  idsEntradasSeleccionadas: number[] = [];
  constructor(
    private service: Pagos,
    private cdr: ChangeDetectorRef,
  ) { }

  ngOnInit(): void {
    this.route?.queryParamMap.subscribe((params) => {
      this.setIdEspectaculo(params.get('idEspectaculo'));
      this.setIdsEntradas(params.get('idsEntradas'));
      this.actualizarImporte();
    });

    if (this.idEspectaculo === undefined && this.isBrowser) {
      const search = new URLSearchParams(window.location.search);
      this.setIdEspectaculo(search.get('idEspectaculo'));
      this.setIdsEntradas(search.get('idsEntradas'));
      this.actualizarImporte();
    }
  }
  ngAfterViewInit(): void {
    this.mountCardElement();
  }

  ngOnDestroy(): void {
    this.destroyCardElement();
  }

  private actualizarImporte(): void {
    this.cantidadEntradas = this.idsEntradasSeleccionadas.length;
    this.importe = this.cantidadEntradas * this.precioUnitario;
  }

  irAPago() {
    if (this.cantidadEntradas <= 0) {
      alert('No hay entradas seleccionadas.');
      return;
    }

    const info = {
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

    console.log('Respuesta Stripe:', response);

    if (response.error) {
      console.log('Error Stripe:', response.error);
      this.cardError = response.error.message || 'Error al procesar el pago.';
      return;
    }

    if (response.paymentIntent?.status === 'succeeded') {
      console.log('Pago correcto en Stripe');

      const confirmPayload = {
        paymentIntentId: response.paymentIntent.id,
        clientSecret: this.clientSecret,
        userToken: localStorage.getItem('authToken') ?? '',
        idEspectaculo: this.idEspectaculo,
        cantidadEntradas: this.idsEntradasSeleccionadas.length,
        idsEntradas: this.idsEntradasSeleccionadas
      };

      console.log('Payload enviado a confirmarPago:', confirmPayload);

      this.service.confirmarPago(confirmPayload).subscribe({
        next: (serviceResponse: any) => {
          console.log('Respuesta confirmarPago:', serviceResponse);
          alert(serviceResponse?.mensaje || 'Pago confirmado');

          if (serviceResponse?.pagoId) {
            window.open(`http://localhost:8080/pagos/${serviceResponse.pagoId}/pdf`, '_blank');
          }
        },
        error: (serviceError: any) => {
          console.log('Error confirmarPago:', serviceError);
          const msg = serviceError?.error?.message || serviceError?.message || 'Error al confirmar el pago';
          alert(msg);
        },
      });

      return;
    }

    this.cardError = 'El pago no se completó correctamente.';
  } catch (error) {
    console.log('Error general en onSubmitPayment:', error);
    this.cardError = 'Se produjo un error al procesar el pago.';
  } finally {
    console.log('Fin del procesamiento');
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

  private setIdsEntradas(rawIds: string | null): void {
    if (!rawIds || rawIds.trim() === '') {
      this.idsEntradasSeleccionadas = [];
      return;
    }

    this.idsEntradasSeleccionadas = rawIds
      .split(',')
      .map(id => Number(id))
      .filter(id => !Number.isNaN(id) && id > 0);
  }
}