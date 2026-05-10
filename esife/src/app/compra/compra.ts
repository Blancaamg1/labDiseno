import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, PLATFORM_ID, ViewChild, inject, NgZone } from '@angular/core';
import { Pagos } from '../pagos';
import { EspectaculosService } from '../espectaculos/espectaculos.service';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EntradaMapaDto } from '../elegir-entradas/elegir-entradas.model';
import { ElegirEntradasStorageService } from '../elegir-entradas/elegir-entradas-storage.service';

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
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  private readonly route = inject(ActivatedRoute, { optional: true });
  private readonly publishableKey = 'pk_test_51T92jkQdO08Nbk2EpzE4U8yNig7EO2Q6etoAl3aWG2NcKeKX0WQL3X7hmjceOzXyfwUz07Enui94aHT2h159EdA3002ovxoko0';

  private stripe: any;
  private elements: any;
  private card: any;
  private isCardMounted = false;

  private precioTotalCentimos = 0;
  importe: number = 0;
  cantidadEntradas: number = 0;
  clientSecret?: string;
  cardError = '';
  isCardReady = false;
  isProcessing = false;
  showPaymentForm = false;
  idEspectaculo?: number;
  entradasMapa: EntradaMapaDto[] = [];

  tiempoRestante: number = 300; // 5 minutos en segundos
  intervalId: any;

  idsEntradasSeleccionadas: number[] = [];
  constructor(
    private service: Pagos,
    private reservasService: EspectaculosService,
    private cdr: ChangeDetectorRef,
    private storageService: ElegirEntradasStorageService
  ) { }

  tokenTurno?: string;
  tokenReserva?: string;

  ngOnInit(): void {
    this.route?.queryParamMap.subscribe((params) => {
      this.setIdEspectaculo(params.get('idEspectaculo'));
      this.setIdsEntradas(params.get('idsEntradas'));
      this.tokenTurno = params.get('tokenTurno') || undefined;
      this.tokenReserva = params.get('tokenReserva') || undefined;
      this.cargarEntradasDesdeBackend();
    });

    if (this.idEspectaculo === undefined && this.isBrowser) {
      const search = new URLSearchParams(window.location.search);
      this.setIdEspectaculo(search.get('idEspectaculo'));
      this.setIdsEntradas(search.get('idsEntradas'));
      this.tokenTurno = search.get('tokenTurno') || undefined;
      this.tokenReserva = search.get('tokenReserva') || undefined;
      this.cargarEntradasDesdeBackend();
    }

    if (this.isBrowser) {
      this.verificarContadorExistente();
    }
  }
  ngAfterViewInit(): void {
    this.mountCardElement();
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.destroyCardElement();
  }

  private verificarContadorExistente(): void {
    const exp = this.storageService.getReservaExpiracion(this.idEspectaculo);
    if (exp) {
      const remaining = Math.floor((exp - Date.now()) / 1000);
      if (remaining > 0) {
        this.tiempoRestante = remaining;
        this.iniciarContador();
      } else {
        this.tiempoRestante = 0;
        this.clearPersistedReservationState();
        this.cardError = 'El tiempo de reserva ha expirado. Las entradas han sido liberadas.';
        this.isProcessing = true;
      }
    } else if (this.idsEntradasSeleccionadas.length > 0) {
      // El usuario acaba de llegar desde la selección sin expiración guardada:
      // iniciamos un timer fresco de 5 minutos.
      const nuevaExpiracion = Date.now() + 300000;
      this.storageService.setReservaExpiracion(nuevaExpiracion, this.idEspectaculo);
      this.tiempoRestante = 300;
      this.iniciarContador();
    } else {
      // No hay entradas ni expiración: no mostramos el mensaje de expirado,
      // simplemente dejamos el contador en su valor inicial (300).
      this.tiempoRestante = 300;
    }
  }

  private iniciarContador(): void {
    if (this.intervalId) return;
    this.intervalId = setInterval(() => {
      const exp = this.storageService.getReservaExpiracion(this.idEspectaculo);
      if (exp) {
        const remaining = Math.floor((exp - Date.now()) / 1000);
        if (remaining > 0) {
          this.tiempoRestante = remaining;
          this.cdr.detectChanges();
        } else {
          this.tiempoRestante = 0;
          clearInterval(this.intervalId);
          this.intervalId = null;
          this.clearPersistedReservationState();
          this.cardError = 'El tiempo de reserva ha expirado. Las entradas han sido liberadas.';
          this.isProcessing = true; // Para deshabilitar el botón
          this.cdr.detectChanges();
        }
      } else {
        this.tiempoRestante = 0;
        clearInterval(this.intervalId);
        this.intervalId = null;
      }
    }, 1000);
  }

  get tiempoFormateado(): string {
    const minutos = Math.floor(this.tiempoRestante / 60);
    const segundos = this.tiempoRestante % 60;
    return `${minutos}:${segundos < 10 ? '0' : ''}${segundos}`;
  }

  private cargarEntradasDesdeBackend(): void {
    if (!this.idEspectaculo) {
      this.actualizarImporte();
      return;
    }

    this.reservasService.obtenerEntradasMapa(this.idEspectaculo).subscribe({
      next: (entradas: EntradaMapaDto[]) => {
        this.entradasMapa = entradas ?? [];
        this.actualizarImporte();
      },
      error: (error) => {
        console.error('Error al cargar precios de entradas:', error);
        this.entradasMapa = [];
        this.actualizarImporte();
      }
    });
  }

  private actualizarImporte(): void {
    this.cantidadEntradas = this.idsEntradasSeleccionadas.length;
    const entradasSeleccionadas = this.entradasMapa.filter(entrada =>
      this.idsEntradasSeleccionadas.includes(entrada.idEntrada)
    );

    if (entradasSeleccionadas.length !== this.idsEntradasSeleccionadas.length) {
      this.precioTotalCentimos = 0;
      this.importe = 0;
      this.cardError = 'Las entradas seleccionadas no pertenecen al espectáculo actual o han expirado.';
      this.isProcessing = true;
      return;
    }

    const entradasInvalidas = entradasSeleccionadas.filter(entrada =>
      this.idEspectaculo != null && entrada.idEspectaculo != null && entrada.idEspectaculo !== this.idEspectaculo
    );

    if (entradasInvalidas.length > 0) {
      this.precioTotalCentimos = 0;
      this.importe = 0;
      this.cardError = 'Las entradas seleccionadas no pertenecen al espectáculo actual.';
      this.isProcessing = true;
      return;
    }

    this.precioTotalCentimos = entradasSeleccionadas
      .reduce((total, entrada) => total + (entrada.precio ?? 0), 0);
    this.importe = this.precioTotalCentimos / 100;
    this.cardError = '';
    if (this.tiempoRestante > 0) {
      this.isProcessing = false;
    }
  }

  irAPago() {
    if (this.cantidadEntradas <= 0) {
      alert('No hay entradas seleccionadas.');
      return;
    }

    if (this.cantidadEntradas > 12) {
      alert('No se permite comprar más de 12 entradas por transacción.');
      return;
    }

    const info = {
      centimos: this.precioTotalCentimos
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
          userToken: this.storageService.getAuthToken(),
          tokenTurno: this.tokenTurno,
          tokenReserva: this.tokenReserva,
          idEspectaculo: this.idEspectaculo,
          cantidadEntradas: this.idsEntradasSeleccionadas.length,
          idsEntradas: this.idsEntradasSeleccionadas
        };

        console.log('Payload enviado a confirmarPago:', confirmPayload);

        this.service.confirmarCompra(confirmPayload).subscribe({
          next: (serviceResponse: any) => {
            console.log('Respuesta confirmarPago:', serviceResponse);
            this.ngZone.run(() => {
              alert(serviceResponse?.mensaje || 'Pago confirmado');
              this.clearPersistedReservationState();
              window.location.href = '/espectaculos';
            });
          },
          error: (serviceError: any) => {
            console.log('Error confirmarPago:', serviceError);
            if (serviceError?.status === 409) {
              this.liberarReservasActuales();
              this.clearPersistedReservationState();
            }
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

  private liberarReservasActuales(): void {
    const userToken = this.storageService.getAuthToken();
    if (!userToken) {
      return;
    }

    this.idsEntradasSeleccionadas.forEach((idEntrada) => {
      this.reservasService.liberar(idEntrada, userToken).subscribe({
        error: () => {
          // Ignoramos errores para no bloquear el flujo de recuperación.
        }
      });
    });
  }

  private clearPersistedSelectionOnly(): void {
    this.storageService.clearSelectionState(this.idEspectaculo);
  }

  private clearPersistedReservationState(): void {
    this.storageService.clearAllReservationState(this.idEspectaculo);
  }
}