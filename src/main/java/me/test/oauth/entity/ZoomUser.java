package me.test.oauth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** api 가이드 기반 작성했으나, 일부 조회되지 않는 데이터는 주석처리함.**/
@Entity
@Table(name="zoomUser")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ZoomUser {

    @Id
    private String id;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(length = 100, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type", referencedColumnName = "type")
    private ZoomLicense licenseInfoList;

    @Column(length = 100)
    private String pmi;

    @Column(length = 50)
    private String timezone;

    private Integer verified;

    @Column(length = 100)
    private String dept;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_client_version", length = 50)
    private String lastClientVersion;

    @Column(length = 500)
    private String pic_url;

    @Column(length = 10)
    private String language;

    @Column(length = 20)
    private String status;

    @Column(name = "role_id", length = 100)
    private String roleId;

    @Column(name = "user_created_at")
    private LocalDateTime userCreatedAt;

    /** 논리삭제를 위해 추가한 필드 */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /** 단독 조회시 확인 가능한 필드 */
    @Column(name = "role_name", length = 100)
    private String roleName;
    @Column(name = "use_pmi")
    private boolean usePmi;
    @Column(name = "personal_meeting_url", length = 500)
    private String personalMeetingUrl;
    @Column(name = "cms_user_id", length = 50)
    private String cmsUserId;
    @Column(name = "jid", length = 256)
    private String jid;
    @Column(name = "group_ids")
    private List<String> groupIds;
    @Column(name = "im_group_ids", length = 256)
    private List<String> ImGroupIds;
    @Column(name = "account_id", length = 100)
    private String accountId;
    @Column(name = "job_title", length = 128)
    private String jobTitle;
    @Column(name = "cost_center", length = 128)
    private String costCenter;
    @Column(name = "company", length = 128)
    private String company;
    @Column(name = "location", length = 100)
    private String location;
    @Column(name = "login_types", length = 100)
    private List<String> loginTypes;
    @Column(name = "account_number", length = 100)
    private String accountNumber;
    @Column(length = 100)
    private String cluster;
    /** 앞으로 사용불가 필드, 조회용 필드 */
    @Column(name = "phone_country", length = 100)
    private String phoneCountry;
    @Column(name = "phone_number", length = 100)
    private String phoneNumber;
    /** 수정용 필드 : DB 저장안함. JSON 출력시 포함 */
    @Transient
    @JsonProperty("phone_numbers")
    public List<PhoneNumber> getPhoneNumbers() {
        if (phoneCountry == null || phoneNumber == null) {
            return new ArrayList<>();
        }
        String code = "";
        String number = this.phoneNumber;
        String label = "Mobile"; // Mobile | Office | Home | Fax
        // "+82 1099991111" 형태라면 국가 코드 분리
        if (number.startsWith("+")) {
            int firstSpace = number.indexOf(" ");
            if (firstSpace > 0) {
                code = number.substring(0, firstSpace);            // +82
                number = number.substring(firstSpace + 1).trim();   // 1099991111
            }
        }
        // phone_country 가 null 이면 guess 로 넘어감
        String country = this.phoneCountry != null ? this.phoneCountry : convertToDialCode(code);
        List<PhoneNumber> list = new ArrayList<>();
        list.add(new PhoneNumber(code, country, label, number));
        return list;
    }

    private String convertToDialCode(String countryCode) {
        return switch (countryCode.toUpperCase()) {
            case "US" -> "+1";
            case "KR" -> "+82";
            default -> "";
        };
    }

    /** 조회 불가능 필드 */
//    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(
//            name = "license_info_list",
//            joinColumns = @JoinColumn(name = "id"))   // 소속 엔티티 PK 값
//    private List<ZoomLicense> licenseInfoList = new ArrayList<>();

    /** JSON 입력 시 "type" 값을 받아서 zoomLicense 셋팅 JSON 출력시 기존 type 필드 유지하면서 추가로 zoom_license 필드에 출력용 데이터 포함
     * @return   "license_info_list": {
     "type": 1,
     "name": "Basic",
     "display_name": "Zoom Meetings 기본"
     }
     */
    @JsonProperty("type")
    public void setType(Integer type) {
        if (type != null) {
            this.licenseInfoList = ZoomLicense.builder().type(type).build();
        }
    }
    @JsonProperty("type")
    public Integer getType() {
        return this.licenseInfoList != null ? this.licenseInfoList.getType() : null;
    }

    // ZoomUser 에 적용할 속성맵
    public static final Map<String, BiConsumer<ZoomUser, String>> zoomUserMapping = Map.of(
            "firstName", ZoomUser::setFirstName,
            "lastName", ZoomUser::setLastName,
            "displayName", ZoomUser::setDisplayName,
            "phoneNumber", ZoomUser::setPhoneNumber,
            "licenseType", ZoomUser::setLicenseInfoList,
            "position", ZoomUser::setJobTitle
    );

    private void setLicenseInfoList(String type) {
        ZoomLicense licenseInfoList = new ZoomLicense(type);
        this.licenseInfoList = licenseInfoList;
    }
}