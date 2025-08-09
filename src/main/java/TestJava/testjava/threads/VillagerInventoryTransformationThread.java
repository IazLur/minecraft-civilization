package TestJava.testjava.threads;

import TestJava.testjava.services.VillagerInventoryTransformationService;

/**
 * Thread qui exécute les transformations d'inventaire des villageois toutes les minutes
 */
public class VillagerInventoryTransformationThread implements Runnable {

    @Override
    public void run() {
        VillagerInventoryTransformationService.executeInventoryTransformationCycle();
    }
}
