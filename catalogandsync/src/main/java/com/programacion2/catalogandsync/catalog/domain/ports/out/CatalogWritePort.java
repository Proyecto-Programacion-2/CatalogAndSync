package com.programacion2.catalogandsync.catalog.domain.ports.out;

import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public interface CatalogWritePort {
    void applyVersion(long version, SyncMode mode, List<ProfessionalCategory> categories,
                      List<Professional> professionals, List<WeeklySchedule> schedules);
}
