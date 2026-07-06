package com.zynboot.jitu.integration.geo;

import com.zynboot.infra.exchange.ExchangeClient;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@ExchangeClient("jitu-geo-provider")
@HttpExchange
public interface UpstreamGeoClient {

    @GetExchange("/service/lbs/search/v1/geo")
    UpstreamGeoResponse geocode(@RequestParam("address") String address);
}
