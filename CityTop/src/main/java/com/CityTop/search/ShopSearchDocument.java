package com.CityTop.search;

import com.CityTop.entity.Shop;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "citytop-shop")
public class ShopSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Long)
    private Long typeId;

    @Field(type = FieldType.Text)
    private String area;

    @Field(type = FieldType.Text)
    private String address;

    @Field(type = FieldType.Long)
    private Long avgPrice;

    @Field(type = FieldType.Integer)
    private Integer sold;

    @Field(type = FieldType.Integer)
    private Integer score;

    public static ShopSearchDocument from(Shop shop) {
        ShopSearchDocument document = new ShopSearchDocument();
        document.setId(shop.getId());
        document.setName(shop.getName());
        document.setTypeId(shop.getTypeId());
        document.setArea(shop.getArea());
        document.setAddress(shop.getAddress());
        document.setAvgPrice(shop.getAvgPrice());
        document.setSold(shop.getSold());
        document.setScore(shop.getScore());
        return document;
    }

    public Shop toShop() {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName(name);
        shop.setTypeId(typeId);
        shop.setArea(area);
        shop.setAddress(address);
        shop.setAvgPrice(avgPrice);
        shop.setSold(sold);
        shop.setScore(score);
        return shop;
    }
}
