package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;

import java.util.List;

public class JpaCatalogWriteAdapter implements CatalogWritePort {
    @Override
    public void applyVersion(long version, SyncMode mode, List<ProfessionalCategory> categories,
                             List<Professional> professionals, List<WeeklySchedule> schedules) {

    }
}
