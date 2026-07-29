package com.moveout.kb_backend.product.client;

import com.moveout.kb_backend.product.entity.KbProduct;
import java.util.List;

public interface KbCoreBankingClient {
    List<KbProduct> fetchKbProducts();
    KbProduct fetchKbProductById(String productId);
}