package com.splitwise.service;

import com.splitwise.dto.GroupDTO;
import com.splitwise.dto.request.CreateGroupRequest;
import com.splitwise.model.Group;
import com.splitwise.model.User;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {
    
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    
    public Group createGroup(CreateGroupRequest request, Long createdByUserId) {
        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setGroupImage(request.getGroupImage());
        group.setCreatedBy(createdBy);
        
        Set<User> members = new HashSet<>();
        members.add(createdBy);
        
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (Long memberId : request.getMemberIds()) {
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + memberId));
                members.add(member);
            }
        }
        
        group.setMembers(members);
        
        return groupRepository.save(group);
    }
    
    public Group getGroupById(Long groupId) {
        // Use custom query that eagerly fetches members
        return groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));
    }
    
    public List<Group> getUserGroups(Long userId) {
        return groupRepository.findGroupsByUserId(userId);
    }
    
    public void addMemberToGroup(Long groupId, Long userId) {
        Group group = getGroupById(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Update both sides of the bidirectional relationship
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
