package com.laodu.p207;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/22
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Order implements Cloneable {
    private TotalPrice totalPrice;
    @Override
    public Object clone() {
        try {
            Order clone = (Order)super.clone();
            clone.setTotalPrice(new TotalPrice(clone.getTotalPrice().getPrice()));
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
