package papapizza.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TanTest {

	private final TanRepo tanRepo;

	@Autowired
	public TanTest(TanRepo tanRepo){
		this.tanRepo = tanRepo;
	}

	//this test is kinda pointless as it tests against random
	//this was just to ensure general functionality
	@Test
	void newTanTest(){
		Tan tan1 = new Tan(new ArrayList<>());
		System.out.println("tan1:"+tan1.getTanNumber());
		Tan tan2 = new Tan(Arrays.asList(tan1));
		System.out.println("tan2:"+tan2.getTanNumber());

		assertTrue(tan1.getTanNumber()>100000 && tan1.getTanNumber()<1000000);
		assertTrue(tan2.getTanNumber()>100000 && tan2.getTanNumber()<1000000);
		assertNotEquals(tan1, tan2);
	}

	//this test on the other hand tests that all tans are unique
	@Test
	void manyTanNoDoubleTest(){
		ArrayList<Tan> tans = new ArrayList<>();
		for(int i=0; i<10000; i++){
			Tan oneMoreTan = new Tan(tans);
			if(tans.contains(oneMoreTan)){
				fail();
			}
			tans.add(oneMoreTan);
		}
		//System.out.println(Arrays.toString(tans.toArray()));
	}

	@Test
	void deleteTanTest(){
		Tan tan1 = new Tan(tanRepo.findAll().toList());
		long id = tan1.getId();
		tanRepo.save(tan1);

		tanRepo.delete(tan1);
		assertFalse(tanRepo.findById(id).isPresent());
		System.out.println(tanRepo.findAll().toList());
	}

}