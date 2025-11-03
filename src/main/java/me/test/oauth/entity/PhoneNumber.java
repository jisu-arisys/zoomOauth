package me.test.oauth.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
/** 사용자정보의 phone_numbers 수정용 객체*/
public class PhoneNumber {
    private String code;
    private String country;
    private String label;
    private String number;
}
