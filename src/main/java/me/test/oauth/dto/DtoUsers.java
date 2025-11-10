package me.test.oauth.dto;

import lombok.*;
import me.test.oauth.entity.User;
import me.test.oauth.entity.ZoomUser;

import java.util.HashMap;
import java.util.Map;


@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoUsers {
    User user;
    ZoomUser zoomUser;


    public DtoUsers getUpdateUser(DtoUsers db) {
        return new DtoUsers(getUpdatedUserByFive(db.getUser()), getUpdatedZoomUserByFive(db.getZoomUser()));
    }

    private boolean isValid(Object value) {
        if (value == null) return false;
        if (value instanceof String s) return !s.trim().isEmpty();
        return true;
    }

    public User getUpdatedUserByFive(User dbUser) {
        if (dbUser == null || this.user == null) return dbUser;

        // 변경 필드만 반영
        if (isValid(user.getUsername()) && !user.getUsername().equals(dbUser.getUsername())) {
            dbUser.setUsername(user.getUsername());
        }

        if (isValid(user.getPosition()) && !user.getPosition().equals(dbUser.getPosition())) {
            dbUser.setPosition(user.getPosition());
        }

        if (isValid(user.getDept().getDeptCode()) && !user.getDept().equals(dbUser.getDept().getDeptCode())) {
            dbUser.setDept(user.getDept());
        }

        if (isValid(user.getActivated()) && !user.getActivated().equals(dbUser.getActivated())) {
            dbUser.setActivated(user.getActivated());
        }

        if (user.getUserType() != null && isValid(user.getUserType().getAuthorityName())) {
            if (!user.getUserType().getAuthorityName().equals(dbUser.getUserType().getAuthorityName())) {
                dbUser.setUserType(user.getUserType());
            }
        }

        return dbUser;
    }

    public ZoomUser getUpdatedZoomUserByFive(ZoomUser dbZoomUser) {
        if (dbZoomUser == null || this.zoomUser == null) return dbZoomUser;

        // ✅ displayName
        if (isValid(zoomUser.getDisplayName()) && !zoomUser.getDisplayName().equals(dbZoomUser.getDisplayName())) {
            dbZoomUser.setDisplayName(zoomUser.getDisplayName());
        }

        // ✅ 부서
        if (isValid(zoomUser.getDept()) && !zoomUser.getDept().equals(dbZoomUser.getDept())) {
            dbZoomUser.setDept(zoomUser.getDept());
        }

        // ✅ 직책
        if (isValid(zoomUser.getJobTitle()) && !zoomUser.getJobTitle().equals(dbZoomUser.getJobTitle())) {
            dbZoomUser.setJobTitle(zoomUser.getJobTitle());
        }

        // ✅ 라이센스
        if (zoomUser.getLicenseInfoList() != null && isValid(zoomUser.getLicenseInfoList().getName())
            && !zoomUser.getLicenseInfoList().getName().equals(dbZoomUser.getLicenseInfoList().getName())) {
                dbZoomUser.setLicenseInfoList(zoomUser.getLicenseInfoList());
        }

        return dbZoomUser;
    }

    public Map<String, Object> getUpdatedZoomUserMapByFive(ZoomUser dbZoomUser) {
        Map<String, Object> zoomMap = new HashMap<>();
        if (dbZoomUser == null || this.zoomUser == null) return zoomMap;

        // ✅ 이름
        if (isValid(zoomUser.getDisplayName()) && !zoomUser.getDisplayName().equals(dbZoomUser.getDisplayName())) {
            zoomMap.put("display_name", zoomUser.getDisplayName());
        }

        // ✅ 부서
        if (isValid(zoomUser.getDept()) && !zoomUser.getDept().equals(dbZoomUser.getDept())) {
            zoomMap.put("dept", zoomUser.getDept());
        }

        // ✅ 직책
        if (isValid(zoomUser.getJobTitle()) && !zoomUser.getJobTitle().equals(dbZoomUser.getJobTitle())) {
            zoomMap.put("job_title", zoomUser.getJobTitle());
        }

        // ✅ 라이센스
        if (zoomUser.getLicenseInfoList() != null && isValid(zoomUser.getLicenseInfoList().getName()) &&
                !zoomUser.getLicenseInfoList().getName().equals(dbZoomUser.getLicenseInfoList().getName())) {
                //4.  vue 에서 라이센스 객체를 전달
                zoomMap.put("type", zoomUser.getLicenseInfoList().getType());
        }
        return zoomMap;
    }
}
