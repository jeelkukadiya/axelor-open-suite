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
package com.axelor.apps.base.service.product;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.BarcodeTypeConfig;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.BarcodeGeneratorService;
import com.axelor.apps.base.service.ProductService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.axelor.utils.service.TranslationService;
import com.google.common.base.Strings;
import com.google.inject.Singleton;

@Singleton
public class ProductUtils {

  protected static final String FULL_NAME_FORMAT = "[%s] %s";

  public void handleProductSave(
      Product product,
      TranslationService translationService,
      AppBaseService appBaseService,
      BarcodeGeneratorService barcodeGeneratorService)
      throws AxelorException {

    // Generate sequence if needed
    if (appBaseService.getAppBase().getGenerateProductSequence()
        && Strings.isNullOrEmpty(product.getCode())) {
      product.setCode(Beans.get(ProductService.class).getSequence(product));
    }

    // Set full name
    product.setFullName(String.format(FULL_NAME_FORMAT, product.getCode(), product.getName()));

    // Handle translations
    if (product.getId() != null) {
      Product oldProduct = Beans.get(ProductRepository.class).find(product.getId());
      translationService.updateFormatedValueTranslations(
          oldProduct.getFullName(), FULL_NAME_FORMAT, product.getCode(), product.getName());
    } else {
      translationService.createFormatedValueTranslations(
          FULL_NAME_FORMAT, product.getCode(), product.getName());
    }

    // Generate barcode if needed
    generateBarcodeIfNeeded(product, appBaseService, barcodeGeneratorService);
  }

  private void generateBarcodeIfNeeded(
      Product product,
      AppBaseService appBaseService,
      BarcodeGeneratorService barcodeGeneratorService) {
    if (product.getBarCode() == null
        && appBaseService.getAppBase().getActivateBarCodeGeneration()) {
      boolean addPadding = false;
      BarcodeTypeConfig barcodeTypeConfig = product.getBarcodeTypeConfig();
      if (!appBaseService.getAppBase().getEditProductBarcodeType()) {
        barcodeTypeConfig = appBaseService.getAppBase().getBarcodeTypeConfig();
      }
      MetaFile barcodeFile =
          barcodeGeneratorService.createBarCode(
              product.getId(),
              "ProductBarCode%d.png",
              product.getSerialNumber(),
              barcodeTypeConfig,
              addPadding);
      if (barcodeFile != null) {
        product.setBarCode(barcodeFile);
      }
    }
  }

  public void handleProductCopy(Product product, Product copy) {
    Beans.get(ProductService.class).copyProduct(product, copy);
    Beans.get(ProductService.class).copyProductCompanies(product.getProductCompanyList(), copy);
  }
}
