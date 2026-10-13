
package kluniversity.com.expense.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.AddMemberRequest;
import kluniversity.com.expense.entity.Group;
import kluniversity.com.expense.entity.GroupMember;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.GroupRepository;
import kluniversity.com.expense.repository.UserRepository;

@Service
public class GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupMemberService(
            GroupMemberRepository groupMemberRepository,
            GroupRepository groupRepository,
            UserRepository userRepository) {
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Group not found"));
    }

    public GroupMember addMember(
            Long groupId,
            AddMemberRequest request,
            String email) {

        User requester = getUser(email);
        Group group = getGroup(groupId);

        if (group.getCreatedBy() == null
                || !group.getCreatedBy().getId().equals(requester.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the group creator can add members");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (groupMemberRepository
                .findByGroupIdAndUserId(groupId, user.getId())
                .isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is already a member of this group");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);

        return groupMemberRepository.save(member);
    }

    public List<GroupMember> getMembers(Long groupId, String email) {
        User requester = getUser(email);
        getGroup(groupId);

        if (!groupMemberRepository
                .existsByGroupIdAndUserId(groupId, requester.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this group");
        }

        return groupMemberRepository.findByGroupId(groupId);
    }
}
