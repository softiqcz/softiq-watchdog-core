package cz.softiq.service.automation.watchdogSearch.service;

import static org.testng.Assert.assertEquals;

import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class WatchDogOfferUtilsTest {

  @DataProvider
  public Object[][] squareMeterHeaders() {
    return new Object[][] {
      {"Prodej bytu 2+1 53 m²", "53"},
      {"Prodej bytu 2+1 53\u00a0m²", "53"},
      {"Prodej bytu 2+1 53\u202fm²", "53"},
      {"Prodej bytu 2+kk 53,5 m2", "53.5"}
    };
  }

  @Test(dataProvider = "squareMeterHeaders")
  public void extractsSquareMetersAcrossHtmlWhitespace(String header, String expectedArea) {
    assertEquals(WatchDogOfferUtils.extractSquareMeter(header).orElseThrow(), expectedArea);
  }

  @Test
  public void calculatesPricePerSquareMeterForExtractedArea() {
    assertEquals(
        WatchDogOfferUtils.calculatePricePerSquareMeter("3500000", "53").orElseThrow(), "66038");
  }

  @Test
  public void populatesMissingMetricsForAnExistingOffer() {
    WatchDogEstatesEntity offer = new WatchDogEstatesEntity();
    offer.setHeader("Prodej bytu 2+1 53\u00a0m²");
    offer.setPrice("3500000");

    WatchDogOfferUtils.populateMissingAreaAndPricePerSquareMeter(offer);

    assertEquals(offer.getSquareMeter(), "53");
    assertEquals(offer.getPricePerSquareMeter(), "66038");
  }
}
