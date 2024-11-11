from random import random, randint
import copy

from settings import *
import data_holder
from message import Message

class User:
    def __init__(self, id: int, location: tuple, world_dimension: tuple, is_adversary=False) -> None:
        self.id = id
        self.is_adversary = is_adversary
        self.contacts = {}
        self.message_storage = []
        self.world_dimension = world_dimension
        self.location = location
        self.people_to_owt = []
        self.inbox_owt_count = 0
        self.pending_incoming_owts = {}
        self.outgoing_owt_dict = {}

        self.unknown_messages_dict = {}
    
    def extend_contacts(self, contact_list: list) -> None:
        if self.is_adversary:
            self._adversary_extend_contacts(contact_list)
        else:
            for contact in contact_list:
                self.contacts[contact] = True

    def _adversary_extend_contacts(self, contact_list: list) -> None:
        new_friend_count = min(len(contact_list) * ADVERSARY_FRIEND_REDUCTION, len(contact_list))
        for i in range(len(contact_list)):
            if i >= new_friend_count: break
            contact = contact_list[i]
            self.contacts[contact] = True

    def generate_message(self, step):
        if self.is_adversary:
            self.generate_adversary_message(step)
        elif random() < USER_MESSAGE_CREATION_RATE:
            self.generate_normal_message(step)

    def add_messages(self, messages, step):
        message_id_set = set()
        for message in self.message_storage:
            message_id_set.add(message.id)
        
        for message in messages:
            if message.id not in message_id_set:
                data_holder.messages_exchanged_steps[step] += 1
                message_copy = copy.deepcopy(message)
                message_copy.decrease_ttl()
                message_copy.received_at = step
                # message_copy.seen_by[self.id] = True
                if self.id not in data_holder.message_seen_counter[message.id]:
                    data_holder.message_seen_counter[message_copy.id].add(self.id)
                    data_holder.message_seen_list_holder[message_copy.id][step] += 1
                self._check_for_percentiles(message_copy, step)
                self.message_storage.append(message_copy)

    def _check_for_percentiles(self, message, step):
        if len(data_holder.message_seen_counter[message.id]) / N > data_holder.highest_percentile_reached_for_message:
            data_holder.highest_percentile_reached_for_message = len(data_holder.message_seen_counter[message.id]) / N

        if (len(data_holder.message_seen_counter[message.id]) / N) > 0.8 and message.id not in data_holder.message_80percentile_holder:
            data_holder.message_propagation_times_80_percentile.append(step - message.created_at)
            data_holder.message_80percentile_holder.add(message.id)
        elif (len(data_holder.message_seen_counter[message.id]) / N) > 0.9 and message.id not in data_holder.message_90percentile_holder:
            data_holder.message_propagation_times_90_percentile.append(step - message.created_at)
            data_holder.message_90percentile_holder.add(message.id)
        elif (len(data_holder.message_seen_counter[message.id]) / N) >= 0.999 and message.id not in data_holder.message_fullpercentile_holder:
            data_holder.message_propagation_times_full.append(step - message.created_at)
            data_holder.message_fullpercentile_holder.add(message.id)

    def generate_adversary_message(self, step):
        self.message_storage.append(Message(self.id, step, is_misinformation=True))
        data_holder.misinformation_count[step] += 1

    def generate_normal_message(self, step):
        self.message_storage.append(Message(self.id, step, is_misinformation=False))
    
    def move(self, move_range):
        # Random move value within move_range
        move_value = (randint(move_range[0][0], move_range[0][1]), randint(move_range[1][0], move_range[1][1]))
        # This line bounds the new location to the boundries of the map
        self.location = (max(min(self.location[0] + move_value[0], self.world_dimension[0] - 1), 0), max(min(self.location[1] + move_value[1], self.world_dimension[1] - 1), 0))

    def act(self, step):
        if self.is_adversary:
            self._adversary_act()
        else:
            self._benign_act(step)

    def _adversary_act(self):
        for message in self.message_storage:
            if message.is_owt and message.owt_recipient == self.id:
                data_holder.owts_recieved_by_adversaries.add(message.author)

    def _benign_act(self, step):
        self._vote_on_messages(step)
        self._create_the_owts(step)
        self._identify_and_respond_to_owts(step)
        self._delete_extra_messages(step)
        

    def _vote_on_messages(self, step):
        for message in self.message_storage:
            if message.is_owt:
                self.inbox_owt_count += 1
                if message.author not in self.pending_incoming_owts and message.owt_recipient == self.id and message.author not in self.contacts:
                    self.pending_incoming_owts[message.author] = 0
                    # data_holder.owt_ttl_when_received.append(message.ttl)
                    data_holder.owt_delay_when_received.append(step - message.created_at)

        for message in self.message_storage:
            voting_condition_1 = not message.is_owt
            voting_condition_2_a = message.author in self.contacts # Author is known
            voting_condition_2_b = False # at least one voter in known
            known_upvoters = 0
            known_downvoters = 0
            unknown_upvoters = 0
            unknown_downvoters = 0
            voting_condition_3 = True # True if we have not voted on this message before
            for voter_id in message.votes.keys():
                if voter_id in self.contacts:
                    voting_condition_2_b = True
                    if message.votes[voter_id] == UPVOTE:
                        known_upvoters += 1
                    else:
                        known_downvoters += 1
                elif voter_id == self.id:
                    voting_condition_3 = False
                else:
                    if message.votes[voter_id] == UPVOTE:
                        unknown_upvoters += 1
                    else:
                        unknown_downvoters += 1
            
            # OWT related only
            if message.author in self.pending_incoming_owts:
                self.pending_incoming_owts[message.author] = self.pending_incoming_owts[message.author] + known_upvoters - known_downvoters

            voted = 0 # 1 for upvote, -1 for downvote, 0 for no vote

            if voting_condition_1 and voting_condition_3:
                if voting_condition_2_a:
                    message.votes[self.id] = UPVOTE
                    voted = 1
                elif voting_condition_2_b:
                    trust_value = known_upvoters - known_downvoters
                    if trust_value >= OWT_MIN_TRUST_VALUE:
                        # People who the user does not know, and have at least the OWT_MIN_TRUST_VALUE will be owt'ed
                        if known_upvoters / (known_upvoters + known_downvoters) >= OWT_MIN_UP_RATIO:
                            self.people_to_owt.append(message.author)

                    if trust_value >= UPVOTE_MIN_TRUST_VALUE: 
                        if known_upvoters / (known_upvoters + known_downvoters) >= UPVOTE_MIN_UP_RATIO:
                            # message.votes[self.id] = UPVOTE
                            # data_holder.message_votes_for_all_messages[message.id][self.id] = UPVOTE
                            voted = self._vote_on_single_message(message, UPVOTE)
                    elif trust_value < 0:
                        # message.votes[self.id] = DOWNVOTE
                        voted = voted = self._vote_on_single_message(message, DOWNVOTE)
                elif random() < USER_VOTING_ON_UNKNOWN_MESSAGES_RATE: # voting on unknown messages
                    self.unknown_messages_dict[message.id] = True
                    if message.is_misinformation:
                        if random() < USER_UPVOTING_ON_MISINFORMATION_RATE:
                            # message.votes[self.id] = UPVOTE
                            voted = self._vote_on_single_message(message, UPVOTE)
                        else:
                            # message.votes[self.id] = DOWNVOTE
                            voted = self._vote_on_single_message(message, DOWNVOTE)
                    else:
                        if random() < USER_UPVOTING_ON_NORMAL_RATE:
                            # message.votes[self.id] = UPVOTE
                            voted = self._vote_on_single_message(message, UPVOTE)
                        else:
                            # message.votes[self.id] = DOWNVOTE
                            voted = self._vote_on_single_message(message, DOWNVOTE)
                        
                if message.is_misinformation and voted == 1:
                    data_holder.upvoted_misinformation_count[step] += 1
                elif message.is_misinformation and voted == -1:
                    data_holder.downvoted_misinformation_count[step] += 1

    def _vote_on_single_message(self, message, votetype):
        message.votes[self.id] = votetype
        data_holder.message_votes_for_all_messages[message.id][self.id] = votetype
        if votetype == UPVOTE:
            return 1
        else:
            return -1

    def _create_the_owts(self, step):
        for owt_receiver_id in self.people_to_owt:
            if owt_receiver_id in self.outgoing_owt_dict: continue
            self.outgoing_owt_dict[owt_receiver_id] = True
            data_holder.total_owt_created += 1
            owt_message = Message(self.id, step, is_misinformation=False, is_owt=True, owt_recipient=owt_receiver_id)
            self.message_storage.append(owt_message)

        self.people_to_owt = []
    
    def _identify_and_respond_to_owts(self, step):
        owts_to_pop = []
        for owt_candidate in self.pending_incoming_owts:
            if owt_candidate in self.outgoing_owt_dict:
                # complete transaction (put them in trusted)
                self.outgoing_owt_dict.pop(owt_candidate)
                self.contacts[owt_candidate] = True
                owts_to_pop.append(owt_candidate)
            elif self.pending_incoming_owts[owt_candidate] >= OWT_MIN_TRUST_VALUE:
                self.contacts[owt_candidate] = True
                self.message_storage.append(Message(self.id, step, is_misinformation=False, is_owt=True, owt_recipient=owt_candidate))
                owts_to_pop.append(owt_candidate)
                data_holder.total_owts_responded_to += 1

        for owt in owts_to_pop:
            self.pending_incoming_owts.pop(owt)

    def _delete_extra_messages(self, step):
        if len(self.message_storage) > MESSAGE_STORAGE_SIZE:
            self.message_storage[:] = [message for message in self.message_storage if self._determine_if_is_recent_enough(message, step)]
        
        if len(self.message_storage) > MESSAGE_STORAGE_SIZE:
            self._internal_deletion_counter = len(self.message_storage) - MESSAGE_STORAGE_SIZE
            self.message_storage[:] = [message for message in self.message_storage if self._determine_if_is_useful(message, step)]
        self.unknown_messages_dict.clear()
    
    def _determine_if_is_recent_enough(self, message, step):
        should_be_kept = True
        if step - message.received_at > OLD_MESSAGE_CUTOFF:
            should_be_kept = False
        return should_be_kept

    def _determine_if_is_useful(self, message, step):
        if self._internal_deletion_counter < 0: return True
        should_be_kept = True
        if message.id in self.unknown_messages_dict:
            should_be_kept = False
            self._internal_deletion_counter -= 1
        
        return should_be_kept
