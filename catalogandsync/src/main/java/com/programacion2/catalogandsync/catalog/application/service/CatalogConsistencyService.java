package com.programacion2.catalogandsync.catalog.application.service;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogConsistencyPort;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CatalogConsistencyService implements CatalogConsistencyPort {
    @Override
    public boolean isStateVerifiable(Optional<Long> version) {
        if (version.isEmpty()){
            return false;
        }
        else
        {return true;}
    }
}
