package net.monetizemyapp.network.api;

import io.reactivex.Single;
import net.monetizemyapp.network.model.step1.Geolocation;
import retrofit2.http.GET;

public interface GeolocationService {

    @GET("json")
    Single<Geolocation> getLocation();
}
