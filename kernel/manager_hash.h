#ifndef __KSU_H_MANAGER_HASH
#define __KSU_H_MANAGER_HASH

#include <linux/types.h>

struct manager_signature {
	unsigned size;
	const char *hash;
};

static const struct manager_signature manager_signatures[] = {
	// Primary (from Kbuild)
	{
		.size = EXPECTED_MANAGER_SIZE,
		.hash = EXPECTED_MANAGER_HASH,
	},
	// weishu
	{
		.size = 0x033b,
		.hash = "c371061b19d8c7d7d6133c6a9bafe198fa944e50c1b31c9d8daa8d7f1fc2d2d6",
	},
	// 5ec1cff
	{
		.size = 384,
		.hash = "7e0c6d7278a3bb8e364e0fcba95afaf3666cf5ff3c245a3b63c8833bd0445cc4",
	},
	// rsuntk
	{
		.size = 0x396,
		.hash = "f415f4ed9435427e1fdf7f1fccd4dbc07b3d6b8751e4dbcec6f19671f427870b",
	},
	// KOWX712
	{
		.size = 0x375,
		.hash = "484fcba6e6c43b1fb09700633bf2fb4758f13cb0b2f4457b80d075084b26c588",
	},
	// /KernelSU-Next
	{
		.size = 0x3e6,
		.hash = "79e590113c4c4c0c222978e413a5faa801666957b1212a328e46c00c69821bf7",
	},
	// pershoot
	{
		.size = 0x338,
		.hash = "f26471a28031130362bce7eebffb9a0b8afc3095f163ce0c75a309f03b644a1f",
	},
};

#define MANAGER_SIGNATURES_COUNT (sizeof(manager_signatures) / sizeof(struct manager_signature))

#endif
