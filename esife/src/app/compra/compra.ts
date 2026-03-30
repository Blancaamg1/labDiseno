import { Component } from '@angular/core';
import { Pagos } from '../pagos';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
declare let Stripe: any;
@Component({
  selector: 'app-compra',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './compra.html',
  styleUrl: './compra.css',
})
export class CompraComponent {

  stripe = Stripe("pk_test_51T92jkQdO08Nbk2EpzE4U8yNig7EO2Q6etoAl3aWG2NcKeKX0WQL3X7hmjceOzXyfwUz07Enui94aHT2h159EdA3002ovxoko0")
  transactionId?: string
  importe: Number = 20.00
  clientSecret?: string
  constructor(private service: Pagos) { }
  irAPago() {
    let info = {
      centimos: Math.floor(this.importe.valueOf() * 100)
    }
    this.service.prepararPago(info).subscribe(
      (response) => {
        this.clientSecret = response?.toString().replace(/^"|"$/g, "");
        this.showForm();
      },
      (error) => {
        console.error('Error al preparar el pago:', error);
      }
    );
  }

  showForm() {
    let elements = this.stripe.elements()
    let style = {
      base: {
        color: "#32325d", fontFamily: 'Arial, sans-serif',
        fontSmoothing: "antialiased", fontSize: "16px",
        "::placeholder": {
          color: "#32325d"
        }
      },
      invalid: {
        fontFamily: 'Arial, sans-serif', color: "#fa755a",
        iconColor: "#fa755a"
      }
    }
    let card = elements.create("card", { style: style })
    card.mount("#card-element")
    card.on("change", function (event: any) {
      document.querySelector("button")!.disabled = event.empty;
      document.querySelector("#card-error")!.textContent =
        event.error ? event.error.message : "";
    });
    let self = this
    let form = document.getElementById("payment-form");
    form!.addEventListener("submit", function (event) {
      event.preventDefault();
      self.payWithCard(card);
    });
    form!.style.display = "block"
  }
  payWithCard(card: any) {
    let self = this
    if (!this.clientSecret) {
      alert('No se pudo obtener client_secret de Stripe. Vuelve a intentarlo.');
      return;
    }

    this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: {
        card: card
      }
    }).then(function (response: any) {
      if (response.error) {
        alert(response.error.message);
      } else {
        if (response.paymentIntent.status === 'succeeded') {
          alert("Pago exitoso");
          self.service.confirmarPago().subscribe({
            next: (response: any) => {
              alert(response)
            },
            error: (response: any) => {
              alert(response)
            }
          });
        }
      }
    });
  }

  requestPrepayment() {
    this.service.prepararPago(this.importe).subscribe({
      next: (response: any) => {
        this.transactionId = response.body
        this.showForm()
      },
      error: (response: any) => {
        alert(response)
      }
    })
  }


}