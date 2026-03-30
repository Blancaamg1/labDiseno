package edu.esi.ds.esientradas.services;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;


@Service
public class PagosService {

    static {
        Stripe.apiKey = "CLAVE";
    } 

    public String prepararPago(Long centimos) throws StripeException{
        PaymentIntentCreateParams params = new PaymentIntentCreateParams.Builder()
            .setCurrency("eur")
            .setAmount(centimos) // centimos = Long
            .build();

        PaymentIntent intent = PaymentIntent.create(params);
        JSONObject json = new JSONObject(intent.toJson());

        String clientSecret = json.getString("client_secret");
        System.out.println("Client secret = " + clientSecret);
        return clientSecret;
    }
    public void confirmarPago(){

    }

}