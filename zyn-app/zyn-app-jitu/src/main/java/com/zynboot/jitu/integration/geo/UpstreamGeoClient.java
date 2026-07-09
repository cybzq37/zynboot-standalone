package com.zynboot.jitu.integration.geo;

import com.zynboot.infra.exchange.ExchangeClient;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@ExchangeClient("jitugeoprovider")
@HttpExchange
public interface UpstreamGeoClient {

    @GetExchange("/service/lbs/search/v1/geo")
    UpstreamGeoResponse geocode(@RequestParam("key") String key,
                                @RequestParam("address") String address);
}
