package com.office.coffeePayer;

import java.util.Comparator;

public class PayerCompare implements Comparator<Payer> {
    public int compare(Payer left, Payer right) {
        return left.getStat().compareTo(right.getStat());
    }
}
