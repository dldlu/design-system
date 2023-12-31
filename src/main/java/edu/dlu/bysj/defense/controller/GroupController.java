package edu.dlu.bysj.defense.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.dlu.bysj.base.model.entity.Student;
import edu.dlu.bysj.base.model.entity.Team;
import edu.dlu.bysj.base.model.entity.TeamUser;
import edu.dlu.bysj.base.model.vo.*;
import edu.dlu.bysj.base.result.CommonResult;
import edu.dlu.bysj.base.util.GradeUtils;
import edu.dlu.bysj.base.util.JwtUtil;
import edu.dlu.bysj.common.service.StudentService;
import edu.dlu.bysj.defense.model.vo.TeamUserVo;
import edu.dlu.bysj.defense.service.TeamService;
import edu.dlu.bysj.defense.service.TeamUserService;
import edu.dlu.bysj.log.annotation.LogAnnotation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * @author XiangXinGang
 * @date 2021/11/7 9:42
 */
@RestController
@RequestMapping(value = "/defenseManagement")
@Api(tags = "分组管理控制器")
@Validated
@Slf4j
public class GroupController {

    private final TeamService teamService;

    private final TeamUserService teamUserService;

    private final StudentService studentService;


    @Autowired
    public GroupController(TeamService teamService,
                           TeamUserService teamUserService,
                           StudentService studentService) {
        this.teamService = teamService;
        this.teamUserService = teamUserService;
        this.studentService = studentService;
    }

    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:50
     * @param majorId
     * @param isSecond
     * @return edu.dlu.bysj.base.result.CommonResult<java.util.List<edu.dlu.bysj.base.model.vo.TeamInfoVo>>
     **/
    @GetMapping(value = "/defence/group/list")
    @LogAnnotation(content = "教师查看专业答辩分组")
    @RequiresPermissions({"group:list"})
    @ApiOperation(value = "查看专业答辩分组")
    public CommonResult<List<TeamInfoVo>> majorDefenseTeam(@Valid @NotNull(message = "专业信息不能为空") Integer majorId,
                                                           @Valid @NotNull(message = "二答信息不能为空") Integer isSecond,
                                                           HttpServletRequest request) {
        int year = LocalDateTime.now().getYear();
        Integer grade = GradeUtils.getGrade(year);
        String jwt = request.getHeader("jwt");
        Integer teacherMajor = JwtUtil.getMajorId(jwt);
        List<Integer> roleIds = JwtUtil.getRoleIds(jwt);
        List<Team> list;
        List<TeamInfoVo> result = new ArrayList<>();
        if(roleIds.contains(4)) {
            list = teamService.list(new QueryWrapper<Team>().eq("major_id", majorId)
                    .eq("grade", grade).eq("is_repeat", isSecond));
        } else {
            if (!teacherMajor.equals(majorId))
                return CommonResult.success(result);
            else
                list = teamService.list(new QueryWrapper<Team>().eq("major_id", majorId)
                        .eq("grade", grade).eq("is_repeat", isSecond));
        }

        if (list != null && !list.isEmpty()) {
            for (Team element : list) {
                TeamInfoVo teamInfoVo = new TeamInfoVo();
                teamInfoVo.setStartDate(element.getStartDate());
                teamInfoVo.setAddress(element.getAddress());
                teamInfoVo.setRequire(element.getRequest());
                teamInfoVo.setEndTime(element.getEndTime());
                DateTimeFormatter dateTimeFormatter1 = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");
                DateTimeFormatter dateTimeFormatter2 = DateTimeFormatter.ofPattern("HH:mm");
                String rangeTime = element.getStartDate().format(dateTimeFormatter1) + " - " + element.getEndTime().format(dateTimeFormatter2);
                teamInfoVo.setRangeTime(rangeTime);
                teamInfoVo.setTeamNumber(element.getTeamNumber());
                teamInfoVo.setGroupId(element.getId());
                result.add(teamInfoVo);
            }
        }
        return CommonResult.success(result);
    }

    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:50
     * @param group
     * @return edu.dlu.bysj.base.result.CommonResult<java.lang.Object>
     **/
    @PostMapping(value = "/defence/group/teacherGroup")
    @LogAnnotation(content = "进行教师分组")
    @RequiresPermissions({"group:teacherGroup"})
    @ApiOperation(value = "提交教师分组")
    public CommonResult<Object> submitTeacherGroup(@Valid @RequestBody TeacherGroupVo group) {
        Integer groupId = group.getGroupId();
        List<TeamUser> teamUsers = new ArrayList<>();
        teamUserService.remove(new QueryWrapper<TeamUser>().eq("team_id", groupId));
        for (int i = 0; i < group.getTeacherId().size(); i++) {
            TeamUser value = new TeamUser();
            value.setInMajor(group.getInMajor().get(i));
            value.setTeamId(groupId);
            value.setUserId(group.getTeacherId().get(i));
            value.setResposiblity(group.getResponsibility().get(i));
            /*1学生, 0非学生*/
            value.setIsStudent(0);
            teamUsers.add(value);
        }
        if (group.getTeacherId().size() == 0)
            CommonResult.success("提交成功");

        boolean flag = teamUserService.saveBatch(teamUsers);
        return flag ? CommonResult.success("提交成功") : CommonResult.failed("提交失败，查看教师信息");
    }

    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:50
     * @param groupVo
     * @param request
     * @return edu.dlu.bysj.base.result.CommonResult<java.lang.Object>
     **/
    @PostMapping(value = "defence/group/modifyGroupInfo")
    @LogAnnotation(content = "新增/修改分组信息")
    @RequiresPermissions({"group:modify"})
    @ApiOperation(value = "新增/修改分组信息")
    public CommonResult<Object> modifyGroupInformation(@Valid @RequestBody ModifyGroupVo groupVo,
                                                       HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        boolean flag = false;
        if (!StringUtils.isEmpty(jwt)) {
            Team team = teamService.getById(groupVo.getId());
            Optional<Team> teamOptional = Optional.ofNullable(team);
            if (!teamOptional.isPresent()) {
                team = new Team();
            }
            if (ObjectUtil.isNotNull(groupVo.getId())) {
                team.setId(groupVo.getId());
                team.setTeamNumber(groupVo.getTeamNumber());
            }
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime start = LocalDateTime.parse(groupVo.getStartDate(),df);
            LocalDateTime end = LocalDateTime.parse(groupVo.getEndTime(),df);
            team.setStartDate(start);
            team.setEndTime(end);
            team.setTeamNumber(groupVo.getTeamNumber());
            team.setAddress(groupVo.getAddress());
            team.setRequest(groupVo.getRequire());
            team.setGrade(GradeUtils.getGrade());
            team.setIsRepeat(groupVo.getIsRepeat() ? 1: 0);
            team.setType(groupVo.getType());
            team.setMajorId(groupVo.getMajorId());

            if (ObjectUtil.isNotNull(groupVo.getId())) {
                /*更新*/
                flag = teamService.updateById(team);
            } else {
                /*保存*/
                flag = teamService.save(team);
            }
        }
        return flag ? CommonResult.success("操作成功") : CommonResult.failed("操作失败");
    }

    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:50
     * @param groupId
     * @return edu.dlu.bysj.base.result.CommonResult<java.util.List<java.util.Map<java.lang.String,java.lang.Integer>>>
     **/
    @GetMapping(value = "/defence/group/teacherGroupList")
    @LogAnnotation(content = "查看该组教师分配情况")
    @RequiresPermissions({"group:teacherGroup"})
    @ApiOperation(value = "查看该组教师分配情况")
    @ApiImplicitParam(name = "groupId", value = "分组id")
    public CommonResult<List<Map<String, Integer>>> obtainTeacherSequenceOfGroup(@NotNull Integer groupId) {
        List<Map<String, Integer>> result = new LinkedList<>();
        List<TeamUser> teamUser = teamUserService.list(new QueryWrapper<TeamUser>()
                .eq("team_id", groupId)
                .eq("is_student", 0));
        if (ObjectUtil.isNotNull(teamUser)) {
            for (TeamUser user : teamUser) {
                Map<String,Integer> map = new HashMap<>();
                map.put("0", user.getInMajor());
                map.put("1", user.getUserId());
                map.put("2", user.getResposiblity());
                result.add(map);
            }
        }
        return CommonResult.success(result);
    }

    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:50
     * @param groupId
     * @return edu.dlu.bysj.base.result.CommonResult<java.util.List<java.util.Map<java.lang.String,java.lang.Object>>>
     **/
    @GetMapping(value = "/defence/group/studentGroupList")
    @LogAnnotation(content = "获取学生答辩分组情况")
    @RequiresPermissions({"group:studentGroup"})
    @ApiOperation(value = "获取学生答辩分组情况")
    @ApiImplicitParam(name = "groupId", value = "分组id")
    public CommonResult<List<Map<String, Object>>> obtainStudentSequenceOfGroup(@NotNull Integer groupId) {

        List<TeamUser> list = teamUserService.list(new QueryWrapper<TeamUser>()
                .eq("team_id", groupId)
                .eq("is_student", 1));
        List<Map<String, Object>> resList = new LinkedList<>();
        if (ObjectUtil.isNotNull(list)) {
            for (TeamUser teamUser : list) {
                Map<String, Object> result = new HashMap<>(16);
                Student student = studentService.getById(teamUser.getUserId());
                Team team = teamService.getById(groupId);
                result.put("serial", teamUser.getSerial());
                result.put("teamNumber",team.getTeamNumber());
                result.put("groupId",groupId);
                result.put("studentName", student.getName());
                result.put("studentId", student.getId());
                resList.add(result);
            }
        }

        return CommonResult.success(resList);
    }

    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:51
     * @param user
     * @return edu.dlu.bysj.base.result.CommonResult<java.lang.Object>
     **/
    @PatchMapping(value = "/defence/group/modifyStudentGroup")
    @LogAnnotation(content = "管理员调整组内序号")
    @RequiresPermissions({"group:revision"})
    @ApiOperation(value = "调整学生组内序号")
    public CommonResult<Object> adjustmentStudentSequenceOfGroup(@RequestBody @Valid TeamUserVo user) {
        boolean flag = false;
        try {
            teamUserService.adjustTeamUserOfStudent(user.getUserId(), user.getTeamNumber(), user.getSerial(), 1);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag ? CommonResult.success("操作成功") : CommonResult.failed("操作失败");
    }

    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:51
     * @param groupIdJson
     * @return edu.dlu.bysj.base.result.CommonResult<java.lang.Object>
     **/
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping(value = "/defence/group/delete")
    @LogAnnotation(content = "删除分组")
    @RequiresPermissions({"group:delete"})
    @ApiOperation(value = "删除分组")
    @ApiImplicitParam(name = "groupId", value = "分组id")
    public CommonResult<Object> deleteGroup(@RequestBody String groupIdJson) {
        String groupId = JSONUtil.parseObj(groupIdJson).get("groupId", String.class);
        /*删除分组并删除该分组下的组员*/
        /*由是否发送异常判断操作是否成功,发生异常事务回滚则该删除操作没有成功,remove()方法在没有记录时返回值为false*/
        teamService.removeById(groupId);
        teamUserService.remove(new QueryWrapper<TeamUser>().eq("team_id", groupId));
        return CommonResult.success("操作成功");
    }


    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:51
     * @param groupId
     * @return edu.dlu.bysj.base.result.CommonResult<edu.dlu.bysj.base.model.vo.ModifyGroupVo>
     **/
    @GetMapping(value = "/defence/group/teacherInfo")
    @RequiresPermissions({"group:teacherInfo"})
    @LogAnnotation(content = "查看分组信息")
    @ApiOperation(value = "查看分组信息")
    public CommonResult<ModifyGroupVo> checkTeacherGroupInfo(
            @Valid @NotNull(message = "分组信息不能为空") Integer groupId) {
        Team team = teamService.getById(groupId);
        ModifyGroupVo groupVo = new ModifyGroupVo();
        groupVo.setAddress(team.getAddress());
        groupVo.setTeamNumber(team.getTeamNumber());
        groupVo.setType(team.getType());
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        groupVo.setStartDate(df.format(team.getStartDate()));
        groupVo.setEndTime(df.format(team.getEndTime()));
        groupVo.setId(team.getId());
        groupVo.setRequire(team.getRequest());
        return CommonResult.success(groupVo);
    }

    /*
     * @Description:
     * @Author: sinre 
     * @Date: 2022/7/3 19:51
     * @param request
     * @return edu.dlu.bysj.base.result.CommonResult<edu.dlu.bysj.base.model.vo.ReplyInformationVo>
     **/
    @GetMapping(value = "/defence/group/studentInfo")
    @RequiresPermissions({"group:studentInfo"})
    @LogAnnotation(content = "查看学生分组信息")
    @ApiOperation(value = "查看学生分组信息")
    public CommonResult<ReplyInformationVo> checkStudentGroupInfo(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        ReplyInformationVo replyInformationVo = null;
        if (!StringUtils.isEmpty(jwt)) {
            /*1表示在该组中为学生*/
            replyInformationVo = teamUserService.checkDefenseGroupOfStudent(JwtUtil.getUserId(jwt), 1);
        }
        return CommonResult.success(replyInformationVo);
    }
}
