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
	// WKSU
	{
		.size = 0x381,
		.hash = "52d52d8c8bfbe53dc2b6ff1c613184e2c03013e090fe8905d8e3d5dc2658c2e4",
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
	// ShirkNeko
	{
		.size = 0x35c,
		.hash = "947ae944f3de4ed4c21a7e4f7953ecf351bfa2b36239da37a34111ad29993eef",
	},
	// Neko
	{
		.size = 0x29c,
		.hash = "946b0557e450a6430a0ba6b6bccee5bc12953ec8735d55e26139b0ec12303b21",
	},
	// ReSukiSU
	{
		.size = 0x377,
		.hash = "d3469712b6214462764a1d8d3e5cbe1d6819a0b629791b9f4101867821f1df64",
	},
	// MamboSU
	{
		.size = 0x384,
		.hash = "a9462b8b98ea1ca7901b0cbdcebfaa35f0aa95e51b01d66e6b6d2c81b97746d8",
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
