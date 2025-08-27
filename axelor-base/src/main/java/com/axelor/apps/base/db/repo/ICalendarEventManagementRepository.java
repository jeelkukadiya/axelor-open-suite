/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.base.db.repo;

import com.axelor.apps.base.db.ICalendarEvent;
import com.axelor.apps.base.ical.ICalendarEventUtils;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.google.inject.Inject;
import javax.persistence.PersistenceException;

public class ICalendarEventManagementRepository extends ICalendarEventRepository {

  private final ICalendarEventUtils eventUtils;

  @Inject
  public ICalendarEventManagementRepository(ICalendarEventUtils eventUtils) {
    this.eventUtils = eventUtils;
  }

  @Override
  public ICalendarEvent save(ICalendarEvent entity) {

    try {
      eventUtils.ensureOrganizer(entity);
      eventUtils.computeSubjectTeam(entity);
      return super.save(entity);
    } catch (Exception e) {
      TraceBackService.traceExceptionFromSaveMethod(e);
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  @Override
  public void remove(ICalendarEvent entity) {
    remove(entity, true);
  }

  public void remove(ICalendarEvent entity, boolean removeRemote) {
    try {
      if (removeRemote) {
        // Use service lazily via Beans to avoid constructor cycles
        eventUtils.removeEventFromIcal(entity);
      }
    } catch (Exception e) {
      TraceBackService.trace(e);
    }
    super.remove(entity);
  }
}
