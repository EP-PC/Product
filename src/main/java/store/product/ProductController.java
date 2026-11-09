package store.product;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product", url = "http://product:8080")
public interface ProductController {

    @PostMapping("/products")
    ResponseEntity<ProductOut> create(
        @RequestBody ProductIn in
    );

    @GetMapping("/products")
    ResponseEntity<List<ProductOut>> findAll();

    @GetMapping("/products/{id}")
    ResponseEntity<ProductOut> findById(
        @PathVariable("id") String id
    );

    @DeleteMapping("/products/{id}")
    ResponseEntity<Void> delete(
        @PathVariable("id") String id
    );

    @GetMapping("/products/health-check")
    ResponseEntity<Void> healthCheck();

}
