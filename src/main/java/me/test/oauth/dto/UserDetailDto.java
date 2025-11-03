package me.test.oauth.dto;

import jakarta.persistence.*;
import lombok.*;
import me.test.oauth.entity.Authority;
import me.test.oauth.entity.User;
import me.test.oauth.entity.UserList;
import me.test.oauth.entity.ZoomLicense;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** join userlist and user for vue */
public class UserDetailDto {

    private String email;

    private String id;
    private String firstName;
    private String lastName;
    private String displayName;
    private ZoomLicense licenseInfoList;
    private String pmi;
    private String timezone;
    private Integer verified;
    private String dept;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginTime;
    private String lastClientVersion;
    private String pic_url;
    private String language;
    private String status;
    private String roleId;
    private LocalDateTime userCreatedAt;

    /** 논리삭제를 위해 추가한 필드 */
    private boolean deleted = false;

    /** 단독 조회시 확인 가능한 필드 */
    private String roleName;
    private boolean usePmi;
    private String personalMeetingUrl;
    private String cmsUserId;
    private String jid;
    private List<String> groupIds;
    private List<String> ImGroupIds;
    private String accountId;
    private String jobTitle;
    private String costCenter;
    private String company;
    private String location;
    private List<String> loginTypes;
    private String accountNumber;
    private String cluster;

    /** 앞으로 사용불가 필드, 조회용 필드 */
    private String phoneCountry;
    private String phoneNumber;

    /** 사내 정보필드 **/
    private Long empNo;
    private String username;
    private String password;
    private String userType;
    private String position;
    private String deptCode;
    private Integer activated;

    // ✅ 전체 목록 변환 함수
    public static List<UserDetailDto> getAllUsers(List<Object[]> rows) {
        return rows.stream()
                .map(row -> {
                    User user = (User) row[0];
                    UserList userList = (UserList) row[1];
                    return UserDetailDto.from(user, userList);
                })
                .toList();
    }

    /** 정적 생성 메서드 (조립 로직) **/
    public static UserDetailDto from(User user, UserList userList) {
        UserDetailDto dto = new UserDetailDto();

        dto.email = user.getEmail();
        dto.id = userList.getId();
        dto.firstName = userList.getFirstName();
        dto.lastName = userList.getLastName();
        dto.displayName = userList.getDisplayName();
        dto.licenseInfoList = userList.getLicenseInfoList();
        dto.pmi = userList.getPmi();
        dto.timezone = userList.getTimezone();
        dto.verified = userList.getVerified();
        dto.dept = userList.getDept();
        dto.createdAt = userList.getCreatedAt();
        dto.lastLoginTime = userList.getLastLoginTime();
        dto.lastClientVersion = userList.getLastClientVersion();
        dto.pic_url = userList.getPic_url();
        dto.language = userList.getLanguage();
        dto.status = userList.getStatus();
        dto.roleId = userList.getRoleId();
        dto.userCreatedAt = userList.getUserCreatedAt();

        dto.phoneCountry = userList.getPhoneCountry();
        dto.phoneNumber = userList.getPhoneNumber();

        dto.roleName = userList.getRoleName();
        dto.usePmi = userList.isUsePmi();
        dto.personalMeetingUrl = userList.getPersonalMeetingUrl();
        dto.cmsUserId = userList.getCmsUserId();
        dto.jid = userList.getJid();
        dto.groupIds = userList.getGroupIds();
        dto.ImGroupIds = userList.getImGroupIds();
        dto.accountId = userList.getAccountId();
        dto.jobTitle = userList.getJobTitle();
        dto.costCenter = userList.getCostCenter();
        dto.company = userList.getCompany();
        dto.location = userList.getLocation();
        dto.loginTypes = userList.getLoginTypes();
        dto.accountNumber = userList.getAccountNumber();
        dto.cluster = userList.getCluster();

        // 사내 사용자 정보
        dto.empNo = user.getEmpNo();
        dto.username = user.getUsername();
        dto.password = user.getPassword();
        dto.userType = user.getUserType().getAuthorityName();
        dto.position = user.getPosition();
        dto.deptCode = user.getDeptCode();
        dto.activated = user.getActivated();

        return dto;
    }
}
