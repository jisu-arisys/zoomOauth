package me.test.oauth.dto;

import lombok.*;
import me.test.oauth.entity.User;
import me.test.oauth.entity.ZoomLicense;
import me.test.oauth.entity.ZoomUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoUsers {
    User user;
    ZoomUser zoomUser;


    public DtoUsers getUpdateUser(DtoUsers db) {
        return new DtoUsers(getUpdatedUserByTen(db.getUser()), getUpdatedZoomUserByFive(db.getZoomUser()));
    }

    private boolean isValid(Object value) {
        if (value == null) return false;
        if (value instanceof String s) return !s.trim().isEmpty();
        return true;
    }

    public User getUpdatedUserByTen(User dbUser) {
        if (dbUser == null || this.user == null) return dbUser;

        // 변경 필드만 반영
        if (isValid(user.getPosition()) && !user.getPosition().equals(dbUser.getPosition())) {
            dbUser.setPosition(user.getPosition());
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

        // ✅ licenseInfoList.name → ZoomLicense 엔티티 찾아 매핑
        if (zoomUser.getLicenseInfoList() != null && isValid(zoomUser.getLicenseInfoList().getName())) {
            String newLicenseName = zoomUser.getLicenseInfoList().getName();
            if (dbZoomUser.getLicenseInfoList() == null ||
                    !newLicenseName.equals(dbZoomUser.getLicenseInfoList().getName())) {

                int type = 0;
                switch (newLicenseName) {
                    case "Basic":
                        type = 1;
                        break;
                    case "Licensed":
                        type = 2;
                        break;
                    case "Unassigned without Meetings Basic":
                        type = 4;
                        break;
                }
                if (type > 0){
                    ZoomLicense dbLicense = dbZoomUser.getLicenseInfoList();
                    dbLicense.setType(type);
                    dbLicense.setName(newLicenseName);
                    dbZoomUser.setLicenseInfoList(dbLicense);
                }
            }
        }

        // ✅ 논리삭제 여부
        dbZoomUser.setDeleted(zoomUser.isDeleted());

        return dbZoomUser;
    }

    public Map<String, Object> getUpdatedZoomUserMapByFive(ZoomUser dbZoomUser, List<ZoomLicense> zoomLicenseList) {
        Map<String, Object> zoomMap = new HashMap<>();
        if (dbZoomUser == null || this.zoomUser == null) return zoomMap;

        // ✅ displayName
        if (isValid(zoomUser.getDisplayName()) && !zoomUser.getDisplayName().equals(dbZoomUser.getDisplayName())) {
            zoomMap.put("display_name", zoomUser.getDisplayName());
        }

        // ✅ 부서
        if (isValid(zoomUser.getDept()) && !zoomUser.getDept().equals(dbZoomUser.getDept())) {
            zoomMap.put("dept", zoomUser.getDept());
        }

        // ✅ licenseInfoList.name → ZoomLicense 엔티티 찾아 매핑
        if (zoomUser.getLicenseInfoList() != null && isValid(zoomUser.getLicenseInfoList().getName())) {
            String newLicenseName = zoomUser.getLicenseInfoList().getName();
            if (dbZoomUser.getLicenseInfoList() == null ||
                    !newLicenseName.equals(dbZoomUser.getLicenseInfoList().getName())) {
                //1. ✔ 서비스 또는 레포지토리를 주입받아 사용해야 함
                // ex) zoomUser.setLicenseInfoList(zoomLicenseRepository.findById(newLicenseName).orElse(null));

                //2. 받아온 목록에서 일치하는 값 찾기
                //ZoomLicense db = (ZoomLicense) zoomLicenseList.stream().filter((zoomLicense -> zoomLicense.getName().equals(newLicenseName)));
                //int type = db.getType();

                //3. 하드코딩 파싱
                int type = 0;
                switch (newLicenseName) {
                    case "Basic":
                        type = 1;
                        break;
                    case "Licensed":
                        type = 2;
                        break;
                    case "Unassigned without Meetings Basic":
                        type = 4;
                        break;
                }
                if (type > 0){
                    zoomMap.put("type", type);
                }
            }
        }
        return zoomMap;
    }
}
