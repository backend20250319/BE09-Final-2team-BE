package com.momnect.userservice.command.client;

import com.momnect.userservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

// **** 예시 파일입니다. 추후 수정해서 사용
// gateway 통해서 접근
@FeignClient(name = "product-service", url = "http://localhost:8000/api/v1/product-service", configuration = FeignClientConfig.class)
public interface ProductClient {

//    // 지역 정보 요청
//    @GetMapping("/products/areas")
//    AreaDTO getAreas(@RequestBody AreaRequest request);
}
