import com.hzl.hadoop.util.LocalDateFormate;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.time.LocalDateTime;

/**
 * description
 *静态方法中不能调用非静态方法，非静态方法中可以调用静态方法，静态方法在类加载的时候初始化
 * @author hzl 2020/01/03 5:04 PM
 */
@Slf4j
public class LocalDateFormateTest {

	@Test
	public void localDateFormate(){
		log.info(LocalDateFormate.localDateFormate("2019-09-09").toString());
	}

	@Test
	public void localDateTimeToString(){
		log.info(LocalDateFormate.localDateTimeToString(LocalDateTime.now(),"yyyy-MM-dd HH:mm:ss"));
	}
}
