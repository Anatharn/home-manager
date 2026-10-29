<script setup>
defineProps({
  compte: {
    type: Object,
    required: true,
  },
  banques: {
    type: Array,
    default: () => [],
  },
  typesCompte: {
    type: Array,
    default: () => [],
  },
});

defineEmits(["open", "delete", "change-banque", "change-type"]);
</script>

<template>
  <article class="compte-card" @click="$emit('open', compte)">
    <div class="compte-card__top">
      <select
        class="compte-card__banque"
        :value="compte.banque?.id ?? ''"
        @click.stop
        @change="$emit('change-banque', compte, $event.target.value || null)"
      >
        <option value="">Sans banque</option>
        <option v-for="banque in banques" :key="banque.id" :value="banque.id">
          {{ banque.nom }}
        </option>
      </select>
      <button class="compte-card__delete" title="Supprimer" @click.stop="$emit('delete', compte)">
        &times;
      </button>
    </div>
    <h3 class="compte-card__libelle">
      {{ compte.libelle }}
      <span v-if="compte.code" class="compte-card__code">{{ compte.code }}</span>
    </h3>
    <select
      class="compte-card__type"
      :value="compte.typeCompte?.id ?? ''"
      @click.stop
      @change="$emit('change-type', compte, $event.target.value || null)"
    >
      <option value="">Sans type</option>
      <option v-for="typeCompte in typesCompte" :key="typeCompte.id" :value="typeCompte.id">
        {{ typeCompte.libelle }}
      </option>
    </select>
    <p v-if="compte.iban" class="compte-card__iban">{{ compte.iban }}</p>
  </article>
</template>

<style scoped>
.compte-card {
  background: var(--color-paper-raised);
  border: 1px solid var(--color-line);
  border-left: 3px solid var(--color-accent);
  padding: 18px 20px;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.compte-card:hover {
  border-color: var(--color-ink);
  border-left-color: var(--color-accent);
}

.compte-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.compte-card__banque {
  font-size: 12px;
  color: var(--color-ink-soft);
  background: none;
  border: none;
  padding: 2px 0;
  font-family: var(--font-body);
  cursor: pointer;
  max-width: 65%;
}

.compte-card__delete {
  background: none;
  border: none;
  color: var(--color-ink-soft);
  font-size: 18px;
  line-height: 1;
  padding: 2px 6px;
}

.compte-card__delete:hover {
  color: var(--color-negative);
}

.compte-card__libelle {
  font-family: var(--font-display);
  font-size: 19px;
  margin: 8px 0 6px;
}

.compte-card__code {
  font-family: var(--font-body);
  font-size: 12px;
  font-weight: normal;
  color: var(--color-ink-soft);
  margin-left: 4px;
}

.compte-card__type {
  display: block;
  font-size: 12px;
  color: var(--color-ink-soft);
  background: none;
  border: none;
  padding: 0 0 6px;
  margin-left: -2px;
  font-family: var(--font-body);
  cursor: pointer;
}

.compte-card__iban {
  font-size: 12px;
  color: var(--color-ink-soft);
  margin: 0;
}
</style>
