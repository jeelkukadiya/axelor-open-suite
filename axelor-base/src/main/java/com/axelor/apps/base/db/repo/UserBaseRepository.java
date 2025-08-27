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

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.base.service.user.UserUtils;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.Query;
import com.google.inject.Inject;
import javax.persistence.PersistenceException;

public class UserBaseRepository extends UserRepository {

  private final UserUtils userUtils;

  @Inject
  public UserBaseRepository(UserUtils userUtils) {
    this.userUtils = userUtils;
  }

  @Override
  public User save(User user) {
    try {
      userUtils.handleUserSave(user);
      user = super.save(user);
      return user;
    } catch (AxelorException e) {
      TraceBackService.traceExceptionFromSaveMethod(e);
      throw new PersistenceException(e.getMessage(), e);
    } catch (Exception e) {
      TraceBackService.traceExceptionFromSaveMethod(e);
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  @Override
  public User copy(User entity, boolean deep) {
    User copy = new User();
    userUtils.handleUserCopy(entity, copy);
    return super.copy(copy, deep);
  }

  @Override
  public void remove(User user) {
    userUtils.handleUserRemove(user);
    super.remove(user);
  }

  @Override
  public User findByEmail(String email) {
    return Query.of(User.class)
        .filter(
            ""
                + "LOWER(self.partner.emailAddress.address) = LOWER(:email) "
                + "OR LOWER(self.email) = LOWER(:email)")
        .bind("email", email)
        .cacheable()
        .fetchOne();
  }

  @Override
  public User findByCodeOrEmail(String codeOrEmail) {
    return Query.of(User.class)
        .filter(
            ""
                + "LOWER(self.code) = LOWER(:codeOrEmail) "
                + "OR LOWER(self.partner.emailAddress.address) = LOWER(:codeOrEmail) "
                + "OR LOWER(self.email) = LOWER(:codeOrEmail)")
        .bind("codeOrEmail", codeOrEmail)
        .cacheable()
        .fetchOne();
  }
}
