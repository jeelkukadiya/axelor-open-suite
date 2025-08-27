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
package com.axelor.apps.base.service.partner;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PartnerAddress;
import com.axelor.apps.base.db.repo.UserBaseRepository;
import com.axelor.apps.base.service.MetaFileService;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.util.List;
import javax.persistence.PersistenceException;
import org.apache.commons.collections.CollectionUtils;

@Singleton
public class PartnerUtils {

  public void handlePartnerOnSave(Partner partner) {
    try {
      Beans.get(PartnerService.class).onSave(partner);
    } catch (AxelorException e) {
      throw new PersistenceException(e.getMessage(), e);
    } catch (Exception e) {
      if (isConstraintViolation(e)) {
        System.err.println(
            "Partner validation skipped due to constraint violation: " + e.getMessage());
      } else {
        throw new PersistenceException(e.getMessage(), e);
      }
    }
    setAddressFlags(partner);
  }

  private void setAddressFlags(Partner partner) {
    try {
      List<PartnerAddress> partnerAddressList = partner.getPartnerAddressList();
      if (CollectionUtils.isNotEmpty(partnerAddressList) && partnerAddressList.size() == 1) {
        PartnerAddress partnerAddress = partnerAddressList.get(0);
        partnerAddress.setIsDefaultAddr(true);
        partnerAddress.setIsDeliveryAddr(true);
        partnerAddress.setIsInvoicingAddr(true);
      }
    } catch (Exception e) {
      System.err.println("Could not set address flags: " + e.getMessage());
    }
  }

  private boolean isConstraintViolation(Exception e) {
    String message = e.getMessage();
    return message != null
        && (message.contains("violates foreign key constraint")
            || message.contains("ConstraintViolationException")
            || message.contains("is still referenced"));
  }

  public void handlePartnerCopy(Partner copy) {
    copy.setPartnerSeq(null);
    copy.setEmailAddress(null);

    try {
      MetaFile picture = copy.getPicture();
      if (picture != null) {
        MetaFile pictureCopy = Beans.get(MetaFileService.class).copyMetaFile(picture);
        copy.setPicture(pictureCopy);
      }
    } catch (Exception e) {
      throw new PersistenceException(e);
    }

    copy.setPartnerAddressList(Lists.newArrayList());
    copy.setBlockingList(null);
    copy.setBankDetailsList(null);
  }

  public void handlePartnerRemove(Partner partner) {
    if (partner.getLinkedUser() != null) {
      User user = Beans.get(UserBaseRepository.class).find(partner.getLinkedUser().getId());
      if (user != null) {
        user.setPartner(null);
        Beans.get(UserBaseRepository.class).save(user);
      }
    }
  }
}
