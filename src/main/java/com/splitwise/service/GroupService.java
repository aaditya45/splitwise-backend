package com.splitwise.service;

import com.splitwise.dto.GroupDTO;
import com.splitwise.dto.request.CreateGroupRequest;
import com.splitwise.exception.ConflictException;
import com.splitwise.exception.ForbiddenException;
import com.splitwise.exception.ResourceNotFoundException;
import com.splitwise.model.Group;
import com.splitwise.model.User;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public Group createGroup(CreateGroupRequest request, Long createdByUserId) {
        User createdBy = userService.getUserById(createdByUserId);

        Group group = new Group();
        group.setName(request.LName().trim());
        group.setDescription(request.LDescription());
        group.setGroupImage(request.LGroupImage());
        group.setCreatedBy(createdBy);

        Set<User> members = new HashSet<>();
        members.add(createdBy);

        if (request.LMemberIds() != null && !request.LMemberIds().isEmpty()) {
            Set<Long> requestedIds = new HashSet<>(request.LMemberIds());
            List<User> found = userRepository.findAllById(requestedIds);
            if (found.size() != requestedIds.size()) {
                Set<Long> foundIds = found.stream().map(User::getUserId).collect(Collectors.toSet());
                requestedIds.removeAll(foundIds);
                throw new ResourceNotFoundException("User not found with id: " + requestedIds);
            }
            members.addAll(found);
        }

        group.setMembers(members);
        return groupRepository.save(group);
    }

    /** Loads a group without any access check. Use {@link #getGroupForMember} for user-facing reads. */
    public Group getGroupById(Long groupId) {
        return groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
    }

    /** Loads a group and verifies that the given user belongs to it. */
    public Group getGroupForMember(Long groupId, Long userId) {
        Group group = getGroupById(groupId);
        boolean isMember = group.getMembers().stream().anyMatch(m -> m.getUserId().equals(userId));
        if (!isMember) {
            throw new ForbiddenException("You are not a member of this group");
        }
        return group;
    }

    /** Cheap membership check for callers that do not need the group itself. */
    @Transactional(readOnly = true)
    public void assertMember(Long groupId, Long userId) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found with id: " + groupId);
        }
        if (groupRepository.countMembership(groupId, userId) == 0) {
            throw new ForbiddenException("You are not a member of this group");
        }
    }

    public List<Group> getUserGroups(Long userId) {
        return groupRepository.findGroupsByUserId(userId);
    }

    public void addMemberToGroup(Long groupId, Long userId, Long requestedByUserId) {
        Group group = getGroupForMember(groupId, requestedByUserId);
        User user = userService.getUserById(userId);

        boolean alreadyMember = group.getMembers().stream().anyMatch(m -> m.getUserId().equals(userId));
        if (alreadyMember) {
            throw new ConflictException("User is already a member of this group");
        }

        group.getMembers().add(user);
        groupRepository.save(group);
    }

    public GroupDTO convertToDTO(Group group) {
        return new GroupDTO(
                group.getGroupId(),
                group.getName(),
                group.getDescription(),
                group.getGroupImage(),
                userService.convertToDTO(group.getCreatedBy()),
                group.getMembers().stream()
                        .map(userService::convertToDTO)
                        .collect(Collectors.toSet()),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
