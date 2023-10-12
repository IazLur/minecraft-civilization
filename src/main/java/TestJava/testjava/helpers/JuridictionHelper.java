package TestJava.testjava.helpers;

import TestJava.testjava.models.ResourceModel;
import TestJava.testjava.repositories.ResourceRepository;

import java.util.Collection;

public class JuridictionHelper {

    public static float calculatePriceForBuy(
            String resourceThatPlayerWant
    ) {
        int totalResources = 0;
        int specificResource = 0;

        for (ResourceModel res : ResourceRepository.getAll()) {
            totalResources += res.getQuantity();
            if (res.getName().equals(resourceThatPlayerWant)) {
                specificResource = res.getQuantity();
            }
        }

        float rarity;
        if (totalResources == 0) {
            rarity = 0;
        } else {
            rarity = 1 - ((float) specificResource / (float) totalResources);
        }

        return (1 + rarity);
    }

    public static float calculatePriceForSell(
            ResourceModel playerSell
    ) {
        int totalResources = 0;
        int specificResource = 0;

        for (ResourceModel res : ResourceRepository.getAll()) {
            totalResources += res.getQuantity();
            if (res.getName().equals(playerSell.getName())) {
                specificResource = res.getQuantity();
            }
        }

        float rarity;
        if (totalResources == 0) {
            rarity = 0;
        } else {
            rarity = 1 - ((float) specificResource / (float) totalResources);
        }

        return (1 + rarity);
    }

}
