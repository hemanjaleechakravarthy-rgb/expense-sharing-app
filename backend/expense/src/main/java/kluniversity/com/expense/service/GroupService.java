
package kluniversity.com.expense.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreateGroupRequest;
import kluniversity.com.expense.entity.Group;
import kluniversity.com.expense.entity.GroupMember;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.GroupRepository;
import kluniversity.com.expense.repository.UserRepository;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    public Group createGroup(CreateGroupRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = new Group();
        group.setName(request.getName());
        group.setCreatedBy(user);

        Group savedGroup = groupRepository.save(group);

        // Automatically add the creator as a group member.
        GroupMember member = new GroupMember();
        member.setGroup(savedGroup);
        member.setUser(user);
        groupMemberRepository.save(member);

        return savedGroup;
    }

    public List<Group> getGroupsByUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<Long, Group> accessibleGroups = new LinkedHashMap<>();

        // Include groups created by this user.
        for (Group group : groupRepository.findByCreatedById(user.getId())) {
            accessibleGroups.put(group.getId(), group);
        }

        // Include groups where this user is a member.
        List<GroupMember> memberships =
                groupMemberRepository.findByUserId(user.getId());

        for (GroupMember membership : memberships) {
            Group group = membership.getGroup();
            if (group != null) {
                accessibleGroups.put(group.getId(), group);
            }
        }

        return new ArrayList<>(accessibleGroups.values());
    }

    public Group renameGroup(
            Long groupId, CreateGroupRequest request, String email) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Group not found"));
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
        if (group.getCreatedBy() == null
                || !group.getCreatedBy().getId().equals(requester.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the group creator can rename this group");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Group name is required");
        }

        group.setName(request.getName().trim());
        return groupRepository.save(group);
    }
}
