package net.monetizemyapp.network.api;

import net.monetizemyapp.network.model.response.IpApiResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface GeolocationService {

    @GET("/json")
    Call<IpApiResponse> getLocation();
}
