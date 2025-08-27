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

import com.axelor.apps.base.service.team.TeamTaskUtils;
import com.axelor.team.db.TeamTask;
import com.axelor.team.db.repo.TeamTaskRepository;
import com.google.inject.Inject;

public class TeamTaskBaseRepository extends TeamTaskRepository {

  private final TeamTaskUtils teamTaskUtils;

  @Inject
  public TeamTaskBaseRepository(TeamTaskUtils teamTaskUtils) {
    this.teamTaskUtils = teamTaskUtils;
  }

  @Override
  public TeamTask save(TeamTask teamTask) {
    teamTaskUtils.handleTaskRecurrence(teamTask);
    teamTaskUtils.validateAndGenerateTasks(teamTask);
    teamTaskUtils.updateNextTasks(teamTask);
    teamTaskUtils.resetTaskFlags(teamTask);

    return super.save(teamTask);
  }

  @Override
  public TeamTask copy(TeamTask entity, boolean deep) {
    TeamTask task = super.copy(entity, deep);
    teamTaskUtils.handleTaskCopy(entity, task);
    return task;
  }
}
