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

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.service.BarcodeGeneratorService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.base.service.observer.ProductFireService;
import com.axelor.apps.base.service.product.ProductUtils;
import com.axelor.utils.service.TranslationService;
import com.google.inject.Inject;
import java.util.Map;
import javax.persistence.PersistenceException;

public class ProductBaseRepository extends ProductRepository {

  private final AppBaseService appBaseService;
  private final TranslationService translationService;
  private final BarcodeGeneratorService barcodeGeneratorService;
  private final ProductFireService productFireService;
  private final ProductUtils productUtils;

  @Inject
  public ProductBaseRepository(
      AppBaseService appBaseService,
      TranslationService translationService,
      BarcodeGeneratorService barcodeGeneratorService,
      ProductFireService productFireService,
      ProductUtils productUtils) {

    this.appBaseService = appBaseService;
    this.translationService = translationService;
    this.barcodeGeneratorService = barcodeGeneratorService;
    this.productFireService = productFireService;
    this.productUtils = productUtils;
  }

  @Override
  public Product save(Product product) {
    try {
      productUtils.handleProductSave(
          product, translationService, appBaseService, barcodeGeneratorService);
      product = super.save(product);
      return product;
    } catch (Exception e) {
      TraceBackService.traceExceptionFromSaveMethod(e);
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  @Override
  public Product copy(Product product, boolean deep) {
    Product copy = super.copy(product, deep);
    productUtils.handleProductCopy(product, copy);
    return copy;
  }

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    productFireService.populate(json, context);
    return super.populate(json, context);
  }
}
