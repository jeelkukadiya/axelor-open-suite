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
package com.axelor.apps.base.service.address;

import com.axelor.apps.base.db.Address;
import com.google.inject.Singleton;

@Singleton
public class AddressUtils {

  public String formatFullName(Address address) {

    return computeFullName(address).toUpperCase();
  }

  private String computeFullName(Address address) {

    StringBuilder fullName = new StringBuilder();

    if (address.getAddressL2() != null) {
      fullName.append(address.getAddressL2()).append(" ");
    }
    if (address.getAddressL3() != null) {
      fullName.append(address.getAddressL3()).append(" ");
    }
    if (address.getAddressL4() != null) {
      fullName.append(address.getAddressL4()).append(" ");
    }
    if (address.getAddressL5() != null) {
      fullName.append(address.getAddressL5()).append(" ");
    }
    if (address.getAddressL6() != null) {
      fullName.append(address.getAddressL6()).append(" ");
    }

    return fullName.toString().trim();
  }

  public boolean needsLatLongUpdate(Address oldAddress, Address newAddress) {
    if (oldAddress == null) return true;
    return !oldAddress.getFullName().equals(newAddress.getFullName());
  }
}
