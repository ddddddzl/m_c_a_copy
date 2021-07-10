package mca.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableMap;
import mca.core.MCA;
import mca.entity.VillagerEntityMCA;
import mca.entity.data.FamilyTree;
import mca.entity.data.Memories;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.Optional;

public class GreetPlayerTask extends Task<VillagerEntityMCA> {
    private static final int talkTime = 10 * 20;
    private int cooldown;
    private PlayerEntity target;
    private boolean talked;
    private int talking;

    public GreetPlayerTask() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryModuleState.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.REGISTERED, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleState.VALUE_PRESENT));
    }

    protected boolean checkExtraStartConditions(ServerWorld world, VillagerEntityMCA villager) {
        if (cooldown > 0) {
            cooldown--;
            return false;
        } else {
            cooldown = 200;
            return getPlayer(villager).isPresent();
        }
    }

    protected void start(ServerWorld world, VillagerEntityMCA villager, long time) {
        Optional<PlayerEntity> player = getPlayer(villager);
        if (player.isPresent()) {
            target = player.get();
            villager.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, target);
            LookTargetUtil.lookAt(villager, target);
            talked = false;
        }
    }

    protected boolean canStillUse(ServerWorld world, VillagerEntityMCA villager, long time) {
        return !talked || talking > 0;
    }

    protected void tick(ServerWorld world, VillagerEntityMCA villager, long time) {
        LookTargetUtil.lookAt(villager, target);
        if (isWithinGreetingDistance(villager, target)) {
            if (!talked) {
                Memories memories = villager.getMemoriesForPlayer(target);
                int day = (int) (villager.world.getTimeOfDay() / 24000L);
                memories.setLastSeen(day);

                String phrase = memories.getHearts() <= MCA.getConfig().greetHeartsThreshold ? "welcomeFoe" : "welcome";
                villager.say(target, phrase, target.getName().asString());
                talked = true;
                talking = talkTime;

                villager.playWelcomeSound();
            }
            talking--;
        } else {
            LookTargetUtil.walkTowards(villager, target, 0.5F, 3);
        }
    }

    protected void stop(ServerWorld world, VillagerEntityMCA villager, long time) {
        target = null;
        villager.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
        villager.getBrain().forget(MemoryModuleType.LOOK_TARGET);
    }

    private Optional<PlayerEntity> getPlayer(VillagerEntityMCA villager) {
        return (Optional<PlayerEntity>) villager.world.getPlayers().stream().filter(p -> shouldGreet(villager, p)).findFirst();
    }

    private boolean shouldGreet(VillagerEntityMCA villager, PlayerEntity player) {
        //first check relationships, only family, friends and foes will greet you
        boolean isRelative = FamilyTree.get(villager.world).isRelative(villager.getUuid(), player.getUuid());
        boolean isSpouse = villager.spouseUUID.get().isPresent() && villager.spouseUUID.get().get().equals(player.getUuid());
        Memories memories = villager.getMemoriesForPlayer(player);
        int day = (int) (villager.world.getTimeOfDay() / 24000L);

        if (isSpouse || isRelative || Math.abs(memories.getHearts()) >= MCA.getConfig().greetHeartsThreshold) {
            int diff = day - memories.getLastSeen();

            if (diff > MCA.getConfig().greetAfterDays && memories.getLastSeen() > 0) {
                return true;
            } else if (diff > 0) {
                //there is a diff, but not long enough
                memories.setLastSeen(day);
            }
        } else {
            //no interest
            memories.setLastSeen(day);
        }
        return false;
    }

    private boolean isWithinGreetingDistance(VillagerEntityMCA villager, PlayerEntity player) {
        BlockPos p = player.getBlockPos();
        BlockPos v = villager.getBlockPos();
        return v.isWithinDistance(p, 3.0D);
    }
}
