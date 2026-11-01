package cz.softiq.service.realiq.service.realiq;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.enums.TypeEnum;
import cz.softiq.service.watchdog.repository.WatchDogEstatesRepository;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

public class RealiqAnalysisTest {

  @Test
  public void aggregateLoadsAllOffersWithoutChangingListingVisibilityQuery() {
    WatchDogEstatesRepository repository = mock(WatchDogEstatesRepository.class);
    WatchDogEntity watchDog = watchDog(830, "Prodej");
    var offers = offers("7000000", "8000000");
    when(repository.findAllWatchDogEstateEntities(watchDog)).thenReturn(offers);
    RealiqPersistenceComponent persistence =
        new RealiqPersistenceComponent(null, null, null, null, repository);

    assertSame(persistence.getOffersForAnalysis(watchDog), offers);
    verify(repository).findAllWatchDogEstateEntities(watchDog);
    verifyNoMoreInteractions(repository);
  }

  @Test
  public void allHiddenAndUnreportedPricesContributeToAllThreeMedians() {
    RealiqPersistenceComponent persistence = mock(RealiqPersistenceComponent.class);
    RealiqComponent component = new RealiqComponent(persistence, null);
    WatchDogEntity rent = watchDog(828, "Pronájem");
    WatchDogEntity renovatedRent = watchDog(829, "Pronájem");
    WatchDogEntity sale = watchDog(830, "Prodej");
    when(persistence.getOffersForAnalysis(rent)).thenReturn(offers("20 000 Kč", "30000", "25000"));
    when(persistence.getOffersForAnalysis(renovatedRent)).thenReturn(offers("29000", "31000"));
    when(persistence.getOffersForAnalysis(sale))
        .thenReturn(offers("6 000 000 Kč", "8000000", "cena na dotaz", null, "0", "1.2.3"));

    var analysis = component.getEstateAnalysis(List.of(sale, renovatedRent, rent));

    assertEquals(analysis.avgPriceBefore(), 25000.0);
    assertEquals(analysis.avgPriceAfter(), 30000.0);
    assertEquals(analysis.avgPriceSell(), 7000000.0);
    verify(persistence).getOffersForAnalysis(rent);
    verify(persistence).getOffersForAnalysis(renovatedRent);
    verify(persistence).getOffersForAnalysis(sale);
    verifyNoMoreInteractions(persistence);
  }

  @Test
  public void emptyAnalysisIsZeroAndRenovatedRentNeverFallsBelowCurrentRent() {
    RealiqPersistenceComponent persistence = mock(RealiqPersistenceComponent.class);
    RealiqComponent component = new RealiqComponent(persistence, null);
    var empty = component.getEstateAnalysis(List.of());
    assertEquals(empty.avgPriceBefore(), 0.0);
    assertEquals(empty.avgPriceAfter(), 0.0);
    assertEquals(empty.avgPriceSell(), 0.0);
    WatchDogEntity rent = watchDog(828, "Pronájem");
    WatchDogEntity renovatedRent = watchDog(829, "Pronájem");
    when(persistence.getOffersForAnalysis(rent)).thenReturn(offers("30000"));
    when(persistence.getOffersForAnalysis(renovatedRent)).thenReturn(offers("20000"));
    var analysis = component.getEstateAnalysis(List.of(rent, renovatedRent));
    assertEquals(analysis.avgPriceBefore(), 30000.0);
    assertEquals(analysis.avgPriceAfter(), 30000.0);
    assertEquals(analysis.avgPriceSell(), 0.0);
  }

  private WatchDogEntity watchDog(int id, String offerType) {
    WatchDogEntity watchDog = new WatchDogEntity();
    watchDog.setId(id);
    watchDog.setRealiqId(31);
    watchDog.setType(TypeEnum.CLIENT_ESTATE_WATCHDOG);
    watchDog.setOfferType(offerType);
    return watchDog;
  }

  private List<WatchDogEstatesEntity> offers(String... prices) {
    return Arrays.stream(prices)
        .map(
            price -> {
              WatchDogEstatesEntity offer = new WatchDogEstatesEntity();
              offer.setPrice(price);
              offer.setIsHiddenFromClientByAdmin(true);
              offer.setIsHiddenFromClientByUser(true);
              offer.setIsReported(false);
              return offer;
            })
        .toList();
  }
}
