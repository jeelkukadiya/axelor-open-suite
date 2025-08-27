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
package com.axelor.apps.base.service.user;

import com.axelor.app.AppSettings;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.repo.PartnerBaseRepository;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.google.inject.Singleton;

@Singleton
public class UserUtils {

  public void handleUserSave(User user) throws AxelorException {
    setDefaultLanguage(user);
    syncEmailFromPartner(user);
    processPasswordChange(user);
  }

  private void setDefaultLanguage(User user) {
    AppSettings appSettings = AppSettings.get();
    String defaultLanguage = appSettings.get("application.locale");

    if (user.getId() == null
        && !(defaultLanguage == null || "".equals(defaultLanguage))
        && (user.getLanguage() == null || "".contentEquals(user.getLanguage()))) {
      user.setLanguage(appSettings.get("application.locale"));
    }
  }

  private void syncEmailFromPartner(User user) {
    if (user.getPartner() != null
        && user.getPartner().getEmailAddress() != null
        && StringUtils.notBlank(user.getPartner().getEmailAddress().getAddress())
        && !user.getPartner().getEmailAddress().getAddress().equals(user.getEmail())) {

      user.setEmail(user.getPartner().getEmailAddress().getAddress());
    }
  }

  private void processPasswordChange(User user) throws AxelorException {
    if (StringUtils.notBlank(user.getTransientPassword())) {
      try {
        Beans.get(UserService.class).processChangedPassword(user);
      } catch (Exception e) {
        throw new AxelorException(e, 4, "Error processing password change: " + e.getMessage());
      }
    }
  }

  public void handleUserCopy(User entity, User copy) {
    copy.setGroup(entity.getGroup());
    copy.setRoles(entity.getRoles());
    copy.setPermissions(entity.getPermissions());
    copy.setMetaPermissions(entity.getMetaPermissions());
    copy.setActiveCompany(entity.getActiveCompany());
    copy.setCompanySet(entity.getCompanySet());
    copy.setLanguage(entity.getLanguage());
    copy.setHomeAction(entity.getHomeAction());
    copy.setSingleTab(entity.getSingleTab());
    copy.setNoHelp(entity.getNoHelp());
  }

  public void handleUserRemove(User user) {
    if (user.getPartner() != null) {
      try {
        PartnerBaseRepository partnerRepo = Beans.get(PartnerBaseRepository.class);
        Partner partner = partnerRepo.find(user.getPartner().getId());
        if (partner != null) {
          partner.setLinkedUser(null);
          partnerRepo.save(partner);
        }
      } catch (Exception e) {
        System.err.println("Error updating partner during user removal: " + e.getMessage());
      }
    }
  }
}
