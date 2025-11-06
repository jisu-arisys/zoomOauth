package me.test.oauth.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.entity.User;
import me.test.oauth.entity.ZoomUser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** join userlist and user for vue */
public class UserDetailDto {

    private static final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    private String email;

    private String id;
    private String firstName;
    private String lastName;
    private String displayName;
    private String licenseDisplayName;
    private String licenseName;
    private String licenseType;
    private String pmi;
    private String timezone;
    private String verified;
    private String dept;
    private String createdAt;
    private String lastLoginTime;
    private String lastClientVersion;
    private String picUrl;
    private String language;
    private String status;
    private String roleId;
    private String userCreatedAt;

    /** 논리삭제를 위해 추가한 필드 */
    private boolean deleted = false;

    /** 단독 조회시 확인 가능한 필드 */
    private String roleName;
    private String usePmi;
    private String personalMeetingUrl;
    private String cmsUserId;
    private String jid;
    private String groupIds;
    private String imGroupIds;
    private String accountId;
    private String jobTitle;
    private String costCenter;
    private String company;
    private String location;
    private String loginTypes;
    private String accountNumber;
    private String cluster;

    /** 앞으로 사용불가 필드, 조회용 필드 */
    private String phoneCountry;
    private String phoneNumber;

    /** 사내 정보필드 **/
    private String empNo;
    private String username;
    private String password;
    private String userType;
    private String position;
    private String deptCode;
    private String activated;

    // ✅ 전체 목록 변환 함수
    public static List<UserDetailDto> getAllUsers(List<Object[]> rows) {
        return rows.stream()
                .map(row -> {
                    User user = (User) row[0];
                    ZoomUser zoomUser = (ZoomUser) row[1];
                    return UserDetailDto.from(user, zoomUser);
                })
                .toList();
    }
    private static String toJsonList(List<?> list) {
        try {
            return list == null ? "[]" : objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
    private static String str(Object o) {
        if (o == null) return "";
        if (o instanceof LocalDateTime dt)
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.valueOf(o);
    }

    /** 정적 생성 메서드 **/
    public static UserDetailDto from(User user, ZoomUser zoomUser) {

        return UserDetailDto.builder()
                .email(str(user.getEmail()))
                .id(str(zoomUser.getId()))
                .firstName(str(zoomUser.getFirstName()))
                .lastName(str(zoomUser.getLastName()))
                .displayName(str(zoomUser.getDisplayName()))
                .licenseDisplayName(str(zoomUser.getLicenseInfoList() != null ? zoomUser.getLicenseInfoList().getDisplayName() :""))
                .licenseName(str(zoomUser.getLicenseInfoList() != null ? zoomUser.getLicenseInfoList().getName() :""))
                .licenseType(str(zoomUser.getLicenseInfoList() != null ? zoomUser.getLicenseInfoList().getType() :""))
                .pmi(str(zoomUser.getPmi()))
                .timezone(str(zoomUser.getTimezone()))
                .verified(str(zoomUser.getVerified()))
                .dept(str(zoomUser.getDept()))
                .createdAt(str(zoomUser.getCreatedAt()))
                .lastLoginTime(str(zoomUser.getLastLoginTime()))
                .lastClientVersion(str(zoomUser.getLastClientVersion()))
                .picUrl(str(zoomUser.getPic_url()))
                .language(str(zoomUser.getLanguage()))
                .status(str(zoomUser.getStatus()))
                .roleId(str(zoomUser.getRoleId()))
                .userCreatedAt(str(zoomUser.getUserCreatedAt()))

                .phoneCountry(str(zoomUser.getPhoneCountry()))
                .phoneNumber(str(zoomUser.getPhoneNumber()))

                .roleName(str(zoomUser.getRoleName()))
                .usePmi(str(zoomUser.isUsePmi()))
                .personalMeetingUrl(str(zoomUser.getPersonalMeetingUrl()))
                .cmsUserId(str(zoomUser.getCmsUserId()))
                .jid(str(zoomUser.getJid()))
                .groupIds(toJsonList(zoomUser.getGroupIds()))
                .imGroupIds(toJsonList(zoomUser.getImGroupIds()))
                .accountId(str(zoomUser.getAccountId()))
                .jobTitle(str(zoomUser.getJobTitle()))
                .costCenter(str(zoomUser.getCostCenter()))
                .company(str(zoomUser.getCompany()))
                .location(str(zoomUser.getLocation()))
                .loginTypes(toJsonList(zoomUser.getLoginTypes()))
                .accountNumber(str(zoomUser.getAccountNumber()))
                .cluster(str(zoomUser.getCluster()))

                .empNo(str(user.getEmpNo()))
                .username(str(user.getUsername()))
                .password(str(user.getPassword()))
                .userType(str(user.getUserType().getAuthorityName()))
                .position(str(user.getPosition()))
                .deptCode(str(user.getDeptCode()))
                .activated(str(user.getActivated()))

                .deleted(zoomUser.isDeleted())
                .build();
    }
}
