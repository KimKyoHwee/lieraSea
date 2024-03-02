package com.server.literasea.service;

import com.server.literasea.dto.BadgeInfoDto;
import com.server.literasea.dto.InventoryInfoDto;
import com.server.literasea.dto.ResponseMainPageDto;
import com.server.literasea.entity.Badge;
import com.server.literasea.entity.Inventory;
import com.server.literasea.entity.Users;
import com.server.literasea.exception.UsersException;
import com.server.literasea.repository.InventoryRepository;
import com.server.literasea.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.server.literasea.exception.UsersExceptionType.NOT_FOUND;

@RequiredArgsConstructor
@Service
public class HomeService {  //TODO: 쿼리개선필요
    //repository-----------------------------------------------------------------
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    //공용------------------------------------------------------------------------
    private Inventory getInventoryByUsers(Users users) {
        return inventoryRepository.findById(users.getInventory().getId())
                .orElseThrow(()->new UsersException(NOT_FOUND));
    }

    //badgeInfo----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<BadgeInfoDto> getBadgeInfoDtoListByUsers(Users users) {
        Inventory usersInventory = getInventoryByUsers(users);
        List<Badge> usersBadges = getBadgeListByInventory(usersInventory);
        return getBadgeDtoListByBadgeList(usersBadges);
    }

    private List<BadgeInfoDto> getBadgeDtoListByBadgeList(List<Badge> badgeList) {
        List<BadgeInfoDto> badgeInfoDtoList = new ArrayList<>();
        for (Badge badge : badgeList) {
            badgeInfoDtoList.add(BadgeInfoDto.badgeToDto(badge));
        }
        return badgeInfoDtoList;
    }

    private List<Badge> getBadgeListByInventory(Inventory inventory) {
        return inventory.getBadges();
    }


    //main-----------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ResponseMainPageDto getMainPageDtoByUsers(Users users) {
        return Users.usersToDto(users);
    }

    //boatInfo-------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public InventoryInfoDto getInventoryInfoDto(Users users) {
        Inventory userInventory = getInventoryByUsers(users);
        return InventoryInfoDto.ToDto(userInventory);
    }
    //-------------------------------------------------------------------------
}
