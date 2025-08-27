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
package com.axelor.apps.base.ical;

import com.axelor.apps.base.db.ICalendarEvent;
import com.axelor.apps.base.db.ICalendarUser;
import com.axelor.apps.base.db.repo.ICalendarEventRepository;
import com.axelor.apps.base.db.repo.ICalendarUserRepository;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Singleton;

@Singleton
public class ICalendarEventUtils {

  public void ensureOrganizer(ICalendarEvent event) {
    if (event.getOrganizer() != null) {
      return;
    }
    User creator = event.getCreatedBy();
    if (creator == null) {
      creator = AuthUtils.getUser();
    }
    if (creator == null) {
      return;
    }

    if (creator.getPartner() != null && creator.getPartner().getEmailAddress() != null) {
      final String email = creator.getPartner().getEmailAddress().getAddress();
      if (email == null) {
        return;
      }

      ICalendarUserRepository repo = Beans.get(ICalendarUserRepository.class);
      ICalendarUser organizer =
          repo.all()
              .filter("self.email = ?1 AND self.user.id = ?2", email, creator.getId())
              .fetchOne();

      if (organizer == null) {
        organizer = new ICalendarUser();
        organizer.setEmail(email);
        organizer.setName(creator.getFullName());
        organizer.setUser(creator);
      }
      event.setOrganizer(organizer);
    }
  }

  public void computeSubjectTeam(ICalendarEvent event) {
    event.setSubjectTeam(event.getSubject());
    if (event.getVisibilitySelect() == ICalendarEventRepository.VISIBILITY_PRIVATE) {
      event.setSubjectTeam(I18n.get("Available"));
      if (event.getDisponibilitySelect() == ICalendarEventRepository.DISPONIBILITY_BUSY) {
        event.setSubjectTeam(I18n.get("Busy"));
      }
    }
  }

  public void removeEventFromIcal(ICalendarEvent event) {
    try {
      Beans.get(ICalendarService.class).removeEventFromIcal(event);
    } catch (Exception e) {
      throw new RuntimeException("Failed to remove event from iCal", e);
    }
  }
}
