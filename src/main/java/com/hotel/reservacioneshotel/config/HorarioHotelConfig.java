package com.hotel.reservacioneshotel.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HorarioHotelConfig {

    @Bean
    Clock relojHotel(@Value("${hotel.zona-horaria}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
