package jp.tkms.util;

import org.junit.jupiter.api.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class FutureArrayListTest {

  /*
  @BeforeAll static void beforeAll() {
  }

  @BeforeEach void beforeEach() {
  }``
   */

  @Test void success() {
    ExecutorService executorService = Executors.newWorkStealingPool();
    FutureArrayList<Integer> object = new FutureArrayList<>();
    assertEquals(object.size(), 0);
    Integer[] array = new Integer[]{123, 456, 789};
    object.add(array[0]);
    object.add(executorService.submit(() -> (array[1] * 2)));
    object.add(() -> (array[2] * 3));
    assertEquals(object.size(), array.length);
    for (int i = 0; i < object.size(); i += 1) {
      assertEquals(object.get(i), array[i] * (i +1));
    }
    int i = 0;
    for (Integer entity : object) {
      assertEquals(entity, array[i] * (i +1));
      i += 1;
    }
    assertTrue(object.contains(array[0]));
    assertTrue(object.remove(array[0]));
    assertFalse(object.contains(array[0]));
    assertFalse(object.remove(array[0]));
    object.clear();
    assertEquals(object.size(), 0);
  }

  /*
  @Test void fail() {
    assertNotNull(object.get(0), "app should have a greeting");
  }

  @AfterEach void afterEach() {
  }

  @AfterAll static void afterAll() {
  }
   */
}
