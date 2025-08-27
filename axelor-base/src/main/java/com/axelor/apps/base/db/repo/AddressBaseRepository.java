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

import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.service.address.AddressService;
import com.axelor.apps.base.service.address.AddressTemplateService;
import com.axelor.apps.base.service.address.AddressUtils;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.db.JPA;
import com.google.inject.Inject;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

public class AddressBaseRepository extends AddressRepository {

  private final AddressService addressService;
  private final AddressTemplateService addressTemplateService;
  private final AddressUtils addressUtils;

  @Inject
  public AddressBaseRepository(
      AddressService addressService,
      AddressTemplateService addressTemplateService,
      AddressUtils addressUtils) {
    this.addressService = addressService;
    this.addressTemplateService = addressTemplateService;
    this.addressUtils = addressUtils;
  }

  @Override
  public Address save(Address entity) {

    try {
      EntityManager em = JPA.em().getEntityManagerFactory().createEntityManager();
      Address oldAddressObject =
          Optional.ofNullable(entity.getId()).map(id -> em.find(Address.class, id)).orElse(null);

      if (addressUtils.needsLatLongUpdate(oldAddressObject, entity)) {
        addressService.updateLatLong(entity);
      }

      addressTemplateService.setFormattedFullName(entity);
      entity.setFullName(addressUtils.formatFullName(entity));
      addressTemplateService.checkRequiredAddressFields(entity);
    } catch (Exception e) {
      TraceBackService.traceExceptionFromSaveMethod(e);
      throw new PersistenceException(e.getMessage(), e);
    }

    return super.save(entity);
  }
}
